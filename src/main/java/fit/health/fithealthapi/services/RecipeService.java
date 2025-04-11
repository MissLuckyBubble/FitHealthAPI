package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.RecipeIngredient;
import fit.health.fithealthapi.model.dto.InferredPreferences;
import fit.health.fithealthapi.model.dto.RecipeSearchRequest;
import fit.health.fithealthapi.model.enums.*;
import fit.health.fithealthapi.repository.RecipeRepository;
import fit.health.fithealthapi.repository.UserPreferenceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final FoodItemService foodItemService;
    private final OntologyService ontologyService;
    private final SharedService sharedService;
    private final EntityManager entityManager;
    private final UserPreferenceRepository userPreferenceRepository;
    private final VerificationPropagationService verificationPropagationService;

    // ===================== Recipe CRUD Operations =====================

    @Transactional
    public Recipe saveRecipe(Recipe recipe) {
        String ontName = sharedService.convertToOntoCase(recipe.getName() + new SimpleDateFormat("yyyyHHmmss").format(new java.util.Date()));
        recipe.setOntologyLinkedName(ontName);
        calculateNutritionalValues(recipe);
        addRecipeToOntology(recipe);
        inferPreferences(recipe);
        inferAllergens(recipe);
        recipe.checkAndUpdateVerification();
        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(Long id, Recipe updatedRecipe) {
        Recipe existingRecipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

        if(!areOntoFieldsEqual(existingRecipe, updatedRecipe)){
            updatedRecipe.setOntologyLinkedName(existingRecipe.getOntologyLinkedName());
            ontologyService.removeDefinedClass(sharedService.convertToOntoCase(updatedRecipe.getOntologyLinkedName()));
            ontologyService.removeObjectPropertyRestrictions(updatedRecipe.getOntologyLinkedName());

            calculateNutritionalValues(updatedRecipe);
            addRecipeToOntology(updatedRecipe);
            inferPreferences(updatedRecipe);
        }
        updateRecipeFields(existingRecipe, updatedRecipe);
        inferAllergens(existingRecipe);
        existingRecipe.checkAndUpdateVerification();
        existingRecipe.setVerifiedByAdmin(true);
        Recipe saved =  recipeRepository.saveAndFlush(existingRecipe);
        System.out.println("id = " + saved.getId());
        System.out.println("Saved verified = " + saved.isVerifiedByAdmin());
        verificationPropagationService.onRecipeUpdated(saved);
        Recipe fresh = recipeRepository.findById(saved.getId()).get();
        System.out.println("Fresh from DB verified = " + fresh.isVerifiedByAdmin());
        return saved;
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

        ontologyService.deleteItem(sharedService.convertToOntoCase(recipe.getOntologyLinkedName()));
        recipeRepository.delete(recipe);
    }

    public List<Recipe> getAllRecipes(){
        return recipeRepository.findAll();
    }

    // ===================== Helper Methods =====================

    private void calculateNutritionalValues(Recipe recipe) {
        float totalCalories = 0, totalFat = 0, totalProtein = 0, totalSalt = 0, totalSugar = 0, totalWeight = 0;

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            FoodItem foodItem = ingredient.getFoodItem();
            float quantityInGrams = ingredient.getUnit().convertToGrams(ingredient.getQuantity());

            totalCalories += (foodItem.getMacronutrients().getCalories() * quantityInGrams) / 100;
            totalFat += (foodItem.getMacronutrients().getFat() * quantityInGrams) / 100;
            totalProtein += (foodItem.getMacronutrients().getProtein() * quantityInGrams) / 100;
            totalSalt += (foodItem.getMacronutrients().getSalt() * quantityInGrams) / 100;
            totalSugar += (foodItem.getMacronutrients().getSugar() * quantityInGrams) / 100;
            totalWeight += quantityInGrams;
        }

        Macronutrients macronutrients = new Macronutrients();
        macronutrients.setCalories(totalCalories);
        macronutrients.setFat(totalFat);
        macronutrients.setProtein(totalProtein);
        macronutrients.setSalt(totalSalt);
        macronutrients.setSalt(totalSugar);
        recipe.setMacronutrients(macronutrients);

        recipe.setTotalWeight(totalWeight);
    }

    private void inferPreferences(Recipe recipe) {
        InferredPreferences inferredPreferences = ontologyService.inferPreferences(recipe.getOntologyLinkedName());
        recipe.setDietaryPreferences(inferredPreferences.getDietaryPreferences());
        recipe.setHealthConditionSuitabilities(inferredPreferences.getHealthConditionSuitabilities());
    }

    private void inferAllergens(Recipe recipe) {
        Set<Allergen> allergens = recipe.getIngredients().stream()
                .flatMap(ingredient -> ingredient.getFoodItem().getAllergens().stream())
                .filter(allergen -> allergen != Allergen.ALLERGEN_FREE)
                .collect(Collectors.toSet());

        if (allergens.isEmpty()) {
            allergens.add(Allergen.ALLERGEN_FREE);
        }
        recipe.getAllergens().clear();
        recipe.getAllergens().addAll(allergens);
    }

    private void addRecipeToOntology(Recipe recipe) {
        ontologyService.createItemType(recipe.getOntologyLinkedName(), "Recipe");
        addRecipeDataProperties(recipe, recipe.getOntologyLinkedName());
        addIngredientsToOntology(recipe, recipe.getOntologyLinkedName());

        ontologyService.saveOntology();
        ontologyService.convertToDefinedClass(recipe.getOntologyLinkedName());
    }

    private void addRecipeDataProperties(Recipe recipe, String recipeName) {
        ontologyService.addDataPropertyRestriction(recipeName, "preparationTime", recipe.getPreparationTime());
        ontologyService.addDataPropertyRestriction(recipeName, "cookingTime", recipe.getCookingTime());
        ontologyService.addDataPropertyRestriction(recipeName, "servingSize", recipe.getServingSize());
        ontologyService.addDataPropertyRestriction(recipeName, "totalCalories", recipe.getMacronutrients().getCalories());
        ontologyService.addDataPropertyRestriction(recipeName, "totalFat", recipe.getMacronutrients().getFat());
        ontologyService.addDataPropertyRestriction(recipeName, "totalProtein", recipe.getMacronutrients().getProtein());
        ontologyService.addDataPropertyRestriction(recipeName, "totalSalt", recipe.getMacronutrients().getSalt());
        ontologyService.addDataPropertyRestriction(recipeName, "totalSugar", recipe.getMacronutrients().getSugar());

        if (recipe.getDescription() != null) {
            ontologyService.addDataPropertyRestriction(recipeName, "description", recipe.getDescription());
        }
    }

    private void addIngredientsToOntology(Recipe recipe, String recipeName) {
        List<String> ingredientsNames = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            ingredientsNames.add(ingredient.getFoodItem().getOntologyLinkedName());
        }
        ontologyService.addIngredientsAsOnlyUnionRestriction(recipeName, ingredientsNames);
    }

    private void updateRecipeFields(Recipe existingRecipe, Recipe updatedRecipe) {
        existingRecipe.setName(updatedRecipe.getName());
        existingRecipe.setDescription(updatedRecipe.getDescription());
        existingRecipe.setPreparationTime(updatedRecipe.getPreparationTime());
        existingRecipe.setCookingTime(updatedRecipe.getCookingTime());
        existingRecipe.setServingSize(updatedRecipe.getServingSize());
        existingRecipe.setMacronutrients(updatedRecipe.getMacronutrients());
        existingRecipe.setDietaryPreferences(updatedRecipe.getDietaryPreferences());
        existingRecipe.setHealthConditionSuitabilities(updatedRecipe.getHealthConditionSuitabilities());
        existingRecipe.setRecipeTypes(updatedRecipe.getRecipeTypes());
        existingRecipe.setIngredients(updatedRecipe.getIngredients());
    }

    private boolean areOntoFieldsEqual(Recipe existingRecipe, Recipe updatedRecipe) {
        if (existingRecipe == null || updatedRecipe == null) {
            return false;
        }

        Set<RecipeIngredient> existingIngredients = existingRecipe.getIngredients();
        Set<RecipeIngredient> updatedIngredients = updatedRecipe.getIngredients();

        if (existingIngredients.size() != updatedIngredients.size()) {
            return false;
        }

        for (RecipeIngredient existingIngredient : existingIngredients) {
            boolean matchFound = updatedIngredients.stream()
                    .anyMatch(updatedIngredient -> areIngredientsEqual(existingIngredient, updatedIngredient));
            if (!matchFound) {
                return false;
            }
        }
        return true;
    }
    private boolean areIngredientsEqual(RecipeIngredient existingIngredient, RecipeIngredient updatedIngredient) {
        return Objects.equals(existingIngredient.getFoodItem().getId(), updatedIngredient.getFoodItem().getId()) &&
                Objects.equals(existingIngredient.getQuantity(), updatedIngredient.getQuantity()) &&
                Objects.equals(existingIngredient.getUnit(), updatedIngredient.getUnit());
    }

    // ===================== Recipe Search Methods =====================

    public List<Recipe> searchRecipes(RecipeSearchRequest searchRequest) {
        List<Recipe> recipeList = recipeRepository.findAll();

        return recipeList.stream()
                .filter(recipe -> matchesDietaryPreferences(recipe, searchRequest.getDietaryPreferences()))
                .filter(recipe -> excludesAllergens(recipe, searchRequest.getAllergens()))
                .filter(recipe -> matchesHealthConditions(recipe, searchRequest.getConditionSuitability()))
                .filter(recipe -> containsIngredients(recipe, searchRequest.getIngredientNames()))
                .filter(recipe -> withinCalorieRange(recipe, searchRequest.getMinCalories(), searchRequest.getMaxCalories()))
                .filter(recipe -> withinTime(recipe, searchRequest.getMaxTotalTime()))
                .filter(recipe -> matchesName(recipe, searchRequest.getName()))
                .filter(recipe -> matchesRecipeTypes(recipe, searchRequest.getRecipeTypes()))
                .sorted(getRecipeComparator(searchRequest.getGoal()))
                .collect(Collectors.toList());

    }

    private Comparator<Recipe> getRecipeComparator(Goal goal) {
        Comparator<Recipe> calorieComparator;

        if (goal == Goal.GAIN_SLOW || goal == Goal.GAIN_FAST) {
            calorieComparator = Comparator.comparing(
                    (Recipe recipe) -> recipe.getMacronutrients().getCalories(), // Explicitly declare type
                    Comparator.nullsLast(Float::compare)
            ).reversed();
        } else {
            calorieComparator = Comparator.comparing(
                    (Recipe recipe) -> recipe.getMacronutrients().getCalories(),
                    Comparator.nullsLast(Float::compare)
            );
        }

        return calorieComparator
                .thenComparing((r -> r.getPreparationTime() + r.getCookingTime()), Comparator.nullsLast(Integer::compare))
                .thenComparing(recipe -> getFavoriteCount(recipe.getId()), Comparator.reverseOrder());
    }


    public int getFavoriteCount(Long recipeId) {
        return userPreferenceRepository.countByItemTypeAndItemIdAndPreferenceType(UserItemType.RECIPE, recipeId, PreferenceType.LIKE);
    }

    public List<Recipe> getAllWithFilters(Map<String, String> filters, String sortField, String sortOrder, int start, int end) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Recipe> query = cb.createQuery(Recipe.class);
        Root<Recipe> recipe = query.from(Recipe.class);

        Predicate predicate = cb.conjunction();

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            predicate = cb.and(predicate, cb.equal(recipe.get(filter.getKey()), filter.getValue()));
        }

        query.where(predicate);

        if ("ASC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.asc(recipe.get(sortField)));
        } else if ("DESC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.desc(recipe.get(sortField)));
        }

        TypedQuery<Recipe> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(start);
        typedQuery.setMaxResults(end - start + 1);

        return typedQuery.getResultList();
    }


    // ===================== Recipe Filter Helper Methods =====================

    private boolean matchesDietaryPreferences(Recipe recipe, List<DietaryPreference> preferences) {
        return preferences == null || recipe.getDietaryPreferences().containsAll(preferences);
    }

    private boolean matchesName(Recipe recipe, String name) {
        return name == null || recipe.getName().contains(name);
    }

    private boolean excludesAllergens(Recipe recipe, List<Allergen> allergens) {
        return allergens == null || Collections.disjoint(recipe.getAllergens(), allergens);
    }

    private boolean matchesHealthConditions(Recipe recipe, List<HealthConditionSuitability> conditions) {
        return conditions == null || recipe.getHealthConditionSuitabilities().containsAll(conditions);
    }

    private boolean matchesRecipeTypes(Recipe recipe, List<RecipeType> recipeTypes) {
        return recipeTypes == null || recipeTypes.isEmpty() || !Collections.disjoint(recipe.getRecipeTypes(), recipeTypes);
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
        boolean meetsMinCalories = minCalories == null || recipe.getMacronutrients().getCalories() >= minCalories;
        boolean meetsMaxCalories = maxCalories == null || recipe.getMacronutrients().getCalories() <= maxCalories;

        return meetsMinCalories && meetsMaxCalories;
    }

    private boolean withinTime(Recipe recipe, Float maxTotalTime) {
        return maxTotalTime == null || (recipe.getPreparationTime() + recipe.getCookingTime()) <= maxTotalTime;
    }

    public Set<Recipe> getRecipesByIds(Set<Long> recipeIds) {
        return new HashSet<>(recipeRepository.findByIdIn(recipeIds));
    }
}
