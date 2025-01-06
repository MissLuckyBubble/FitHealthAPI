package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.IngredientNotFoundException;
import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.RecipeIngredient;
import fit.health.fithealthapi.model.dto.InferredPreferences;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.repository.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private SharedService sharedService;

    /**
     * Save a new Recipe and add it to the ontology.
     */
    @Transactional
    public Recipe saveRecipe(Recipe recipe) {
        Set<RecipeIngredient> ingredients = validateIngredients(recipe.getIngredients());
        for (RecipeIngredient ingredient : ingredients) {
            ingredient.setRecipe(recipe);
        }
        recipe.setIngredients(ingredients);

        calculateNutritionalValues(recipe);

        addRecipeToOntology(recipe);

        inferPreferences(recipe);
        inferAllergens(recipe);

        return recipeRepository.save(recipe);
    }

    /**
     * Update an existing Recipe and update its ontology representation.
     */
    @Transactional
    public Recipe updateRecipe(Long id, Recipe updatedRecipe) {
        Recipe existingRecipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

        Set<RecipeIngredient> ingredients = validateIngredients(updatedRecipe.getIngredients());
        updatedRecipe.setIngredients(ingredients);

        if (!existingRecipe.getName().equals(updatedRecipe.getName())) {
            ontologyService.renameItem(sharedService.convertToOntologyCase(existingRecipe.getName()), sharedService.convertToOntologyCase(updatedRecipe.getName()));
        }

        ontologyService.removeDefinedClass(sharedService.convertToOntologyCase(updatedRecipe.getName()));
        ontologyService.removeObjectPropertyRestrictions(updatedRecipe.getName());

        calculateNutritionalValues(updatedRecipe);
        addRecipeToOntology(updatedRecipe);

        inferPreferences(updatedRecipe);
        inferAllergens(updatedRecipe);

        existingRecipe.setName(updatedRecipe.getName());
        existingRecipe.setDescription(updatedRecipe.getDescription());
        existingRecipe.setPreparationTime(updatedRecipe.getPreparationTime());
        existingRecipe.setCookingTime(updatedRecipe.getCookingTime());
        existingRecipe.setServingSize(updatedRecipe.getServingSize());
        existingRecipe.setCalories(updatedRecipe.getCalories());
        existingRecipe.setFatContent(updatedRecipe.getFatContent());
        existingRecipe.setProteinContent(updatedRecipe.getProteinContent());
        existingRecipe.setSaltContent(updatedRecipe.getSaltContent());
        existingRecipe.setSugarContent(updatedRecipe.getSugarContent());
        existingRecipe.setDietaryPreferences(updatedRecipe.getDietaryPreferences());

        return recipeRepository.save(existingRecipe);
    }

    private void addRecipeToOntology(Recipe recipe) {
        System.out.println("Original Recipe Name: [" + recipe.getName() + "]");

        String pascalCaseName = sharedService.convertToPascalCase(recipe.getName());
        System.out.println("PascalCase Recipe Name: [" + pascalCaseName + "]");

        String recipeName = sharedService.convertToOntologyCase(recipe.getName());
        System.out.println("Ontology Case Recipe Name: [" + recipeName + "]");

        ontologyService.createItemType(recipeName, "Recipe");

        ontologyService.addDataPropertyRestriction(recipeName, "preparationTime", recipe.getPreparationTime());
        ontologyService.addDataPropertyRestriction(recipeName, "cookingTime", recipe.getCookingTime());
        ontologyService.addDataPropertyRestriction(recipeName, "servingSize", recipe.getServingSize());
        ontologyService.addDataPropertyRestriction(recipeName, "totalCalories", recipe.getCalories());
        ontologyService.addDataPropertyRestriction(recipeName, "fatContent", recipe.getFatContent());
        ontologyService.addDataPropertyRestriction(recipeName, "proteinContent", recipe.getProteinContent());
        ontologyService.addDataPropertyRestriction(recipeName, "saltContent", recipe.getSaltContent());
        ontologyService.addDataPropertyRestriction(recipeName, "sugarContent", recipe.getSugarContent());

        if (recipe.getDescription() != null) {
            ontologyService.addDataPropertyRestriction(recipeName, "description", recipe.getDescription());
        }

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            ontologyService.addObjectPropertyRestriction(recipeName, "hasIngredient", ingredient.getFoodItem().getName());
        }

        ontologyService.saveOntology();
        ontologyService.convertToDefinedClass(recipeName);
    }


    private void calculateNutritionalValues(Recipe recipe) {
        float totalCalories = 0;
        float totalFat = 0;
        float totalProtein = 0;
        float totalSalt = 0;
        float totalSugar = 0;
        float totalWeight = 0;

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            FoodItem foodItem = ingredient.getFoodItem();

            // Convert quantity to grams
            float quantityInGrams = ingredient.getUnit().convertToGrams(ingredient.getQuantity());

            // Accumulate nutritional values
            totalCalories += (foodItem.getCaloriesPer100g() * quantityInGrams) / 100;
            totalFat += (foodItem.getFatContent() * quantityInGrams) / 100;
            totalProtein += (foodItem.getProteinContent() * quantityInGrams) / 100;
            totalSalt += (foodItem.getSaltContent() * quantityInGrams) / 100;
            totalSugar += (foodItem.getSugarContent() * quantityInGrams) / 100;
            totalWeight += quantityInGrams;
        }

        recipe.setCalories(totalCalories);
        recipe.setFatContent(totalFat);
        recipe.setProteinContent(totalProtein);
        recipe.setSaltContent(totalSalt);
        recipe.setSugarContent(totalSugar);
        recipe.setTotalWeight(totalWeight);

    }

    private void inferPreferences(Recipe recipe) {
        InferredPreferences inferredPreferences = ontologyService.inferPreferences(sharedService.convertToOntologyCase(recipe.getName()));
        recipe.setDietaryPreferences(inferredPreferences.getDietaryPreferences());
        recipe.setHealthConditionSuitability(inferredPreferences.getHealthConditionSuitabilities());
    }

    private void inferAllergens(Recipe recipe) {
        Set<Allergen> allergens = recipe.getIngredients().stream()
                .flatMap(ingredient -> ingredient.getFoodItem().getAllergens().stream())
                .filter(allergen -> allergen != Allergen.ALLERGEN_FREE)
                .collect(Collectors.toSet());

        if (allergens.isEmpty()) {
            allergens.add(Allergen.ALLERGEN_FREE);
        }
        recipe.setAllergens(allergens);
    }


    private Set<RecipeIngredient> validateIngredients(Set<RecipeIngredient> ingredients) {
        for (RecipeIngredient ingredient : ingredients) {
            Optional<FoodItem> optionalFoodItem = foodItemService.findByName(ingredient.getFoodItem().getName());
            if (optionalFoodItem.isPresent()) {
                ingredient.setFoodItem(optionalFoodItem.get());
            }else {
                throw new IngredientNotFoundException(ingredient.getFoodItem().getName());
            }
        }
        return ingredients;
    }

    @Transactional(readOnly = true)
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Recipe getRecipeById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found with ID: " + id));
    }

    @Transactional
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found with ID: " + id));

        ontologyService.deleteItem(sharedService.convertToOntologyCase(recipe.getName()));

        recipeRepository.delete(recipe);
    }

    public List<Recipe> searchRecipes(List<DietaryPreference> dietaryPreferences,
                                      List<Allergen> allergens,
                                      List<HealthConditionSuitability> healthConditions,
                                      List<String> ingredientNames,
                                      Float minCalories,
                                      Float maxCalories,
                                      Float maxTotalTime,
                                      String name)
    {
        List<Recipe> recipeList = recipeRepository.findAll();

        return recipeList.stream()
                .filter(recipe -> matchesDietaryPreferences(recipe, dietaryPreferences))
                .filter(recipe -> excludesAllergens(recipe, allergens))
                .filter(recipe -> matchesHealthConditions(recipe, healthConditions))
                .filter(recipe -> containsIngredients(recipe, ingredientNames))
                .filter(recipe -> withinCalorieRange(recipe, minCalories, maxCalories))
                .filter(recipe -> withinTime(recipe, maxTotalTime))
                .filter(recipe -> matchesName(recipe,name))
                .collect(Collectors.toList());
    }

    private boolean matchesDietaryPreferences(Recipe recipe, List<DietaryPreference> preferences) {
        return preferences == null || recipe.getDietaryPreferences().containsAll(preferences);
    }

    private boolean matchesName(Recipe recipe, String name) {
        return name.isBlank() || recipe.getName().contains(name);
    }

    private boolean excludesAllergens(Recipe recipe, List<Allergen> allergens) {
        return allergens == null || Collections.disjoint(recipe.getAllergens(), allergens);
    }

    private boolean matchesHealthConditions(Recipe recipe, List<HealthConditionSuitability> conditions) {
        return conditions == null || recipe.getHealthConditionSuitability().containsAll(conditions);
    }

    private boolean containsIngredients(Recipe recipe, List<String> ingredientNames) {
        if (ingredientNames == null || ingredientNames.isEmpty()) return true;

        Set<String> recipeIngredientNames = recipe.getIngredients().stream()
                .map(ingredient -> ingredient.getFoodItem().getName().toLowerCase())
                .collect(Collectors.toSet());

        return ingredientNames.stream()
                .map(String::toLowerCase)
                .allMatch(recipeIngredientNames::contains);
    }

    private boolean withinCalorieRange(Recipe recipe, Float minCalories, Float maxCalories) {
        boolean meetsMinCalories = (minCalories == null || recipe.getCalories() >= minCalories);
        boolean meetsMaxCalories = (maxCalories == null || recipe.getCalories() <= maxCalories);

        return meetsMinCalories && meetsMaxCalories;
    }

    private boolean withinTime(Recipe recipe, Float maxTotalTime) {
        if(maxTotalTime == null){
            return true;
        }
        return recipe.getPreparationTime() + recipe.getCookingTime() <= maxTotalTime;
    }

}
