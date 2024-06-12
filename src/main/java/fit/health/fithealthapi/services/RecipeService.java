package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Recipe;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    @Autowired
    DataPropertyService dataPropertyService;
    @Autowired
    ObjectPropertyService objectPropertyService;
    @Autowired
    OntologyService ontologyService;
    @Autowired
    FoodItemService foodItemService;

    private OWLOntologyManager ontoManager;
    private OWLDataFactory dataFactory;
    private OWLOntology ontology;
    private String ontologyIRIStr;

    public RecipeService(DataPropertyService dataPropertyService, ObjectPropertyService objectPropertyService, OntologyService ontologyService, FoodItemService foodItemService) {
        this.dataPropertyService = dataPropertyService;
        this.objectPropertyService = objectPropertyService;
        this.ontologyService = ontologyService;
        this.foodItemService = foodItemService;

        init();
    }

    private void init() {
        ontoManager = ontologyService.getOntoManager();
        dataFactory = ontologyService.getDataFactory();
        ontology = ontologyService.getOntology();
        ontologyIRIStr = ontologyService.getOntologyIRIStr();
    }

    public List<Recipe> getRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        OWLClass recipeClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "Recipe"));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        for (OWLNamedIndividual individual : individuals) {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(recipeClass))) {
                getRecipe(recipes, individual);
            }
        }
        return recipes;
    }

    private void getRecipe(List<Recipe> recipes, OWLNamedIndividual individual) {
        Recipe recipe = new Recipe();
        recipe.setId(individual.getIRI().toString());
        recipe.setRecipeName(dataPropertyService.getDataPropertyValue(individual, "recipeName"));
        recipe.setCookingTime(dataPropertyService.getIntValue(individual, "cookingTime"));
        recipe.setPreparationTime(dataPropertyService.getIntValue(individual, "preparationTime"));
        recipe.setServingSize(dataPropertyService.getIntValue(individual, "servingSize"));
        recipe.setDescription(dataPropertyService.getDataPropertyValue(individual, "description"));
        recipe.setCaloriesPer100gram(dataPropertyService.getFloatValue(individual, "caloriesPer100gram"));
        recipe.setFatContent(dataPropertyService.getFloatValue(individual, "fatContent"));
        recipe.setProteinContent(dataPropertyService.getFloatValue(individual, "proteinContent"));
        recipe.setSugarContent(dataPropertyService.getFloatValue(individual, "sugarContent"));

        List<String> ingredientNames = objectPropertyService.getObjectPropertyValues(individual, "hasIngredient")
                .stream()
                .map(ontologyService::getFragment)
                .collect(Collectors.toList());
        recipe.setIngredients(ingredientNames);

        // Calculate allergens from ingredients
        //calculateAllergens(recipe, ingredientNames);

        recipe.setDietaryPreferences(getDietaryPreferences(individual));
        recipes.add(recipe);
    }

    private void calculateNutritionAndAllergens(Recipe recipe, List<String> ingredientNames) {
        float totalCalories = 0;
        float totalFat = 0;
        float totalProtein = 0;
        float totalSugar = 0;

        for (String ingredientName : ingredientNames) {
            Optional<FoodItem> foodItem = foodItemService.getFoodItems().stream()
                    .filter(item -> ingredientName.replace("_"," ").equals(item.getFoodName()))
                    .findFirst();

            if (foodItem.isPresent()) {
                FoodItem item = foodItem.get();
                totalCalories += item.getCaloriesPer100gram();
                totalFat += item.getFatContent();
                totalProtein += item.getProteinContent();
                totalSugar += item.getSugarContent();
            }
        }

        int ingredientCount = ingredientNames.size();
        recipe.setCaloriesPer100gram(totalCalories / ingredientCount);
        recipe.setFatContent(totalFat / ingredientCount);
        recipe.setProteinContent(totalProtein / ingredientCount);
        recipe.setSugarContent(totalSugar / ingredientCount);
    }

    private void calculateAllergens(Recipe recipe, List<String> ingredientNames) {
        Set<String> allergens = new HashSet<>();

        for (String ingredientName : ingredientNames) {
            Optional<FoodItem> foodItem = foodItemService.getFoodItems().stream()
                    .filter(item -> ingredientName.equals(item.getFoodName()))
                    .findFirst();

            if (foodItem.isPresent()) {
                FoodItem item = foodItem.get();
                if (item.getAllergens() != null) {
                    allergens.addAll(item.getAllergens());
                }
            }
        }

        //recipe.setAllergens(new ArrayList<>(allergens));
    }

    private List<String> getDietaryPreferences(OWLNamedIndividual individual) {
        List<String> preferences = new ArrayList<>();
        for (Map.Entry<OWLClass, String> entry : ontologyService.getDietaryPreferencesMap().entrySet()) {
            if (ontologyService.isIndividualOfClass(individual, entry.getKey())) {
                preferences.add(ontologyService.getFragment(entry.getValue()));
            }
        }
        return preferences;
    }

    public void createRecipe(Recipe recipe) {
        OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + recipe.getRecipeName().replace(" ", "_")));
        if (ontology.containsIndividualInSignature(recipeIndividual.getIRI())) {
            throw new CustomException("Recipe already exists");
        }
        OWLClass recipeClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "Recipe"));

        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(recipeClass, recipeIndividual);
        ontoManager.addAxiom(ontology, classAssertion);

        dataPropertyService.addDataProperty(recipeIndividual, "recipeName", recipe.getRecipeName());
        dataPropertyService.addDataProperty(recipeIndividual, "cookingTime", recipe.getCookingTime());
        dataPropertyService.addDataProperty(recipeIndividual, "preparationTime", recipe.getPreparationTime());
        dataPropertyService.addDataProperty(recipeIndividual, "servingSize", recipe.getServingSize());
        dataPropertyService.addDataProperty(recipeIndividual, "description", recipe.getDescription());

        // Calculate and save nutrition
        calculateNutritionAndAllergens(recipe, recipe.getIngredients());
        dataPropertyService.addDataProperty(recipeIndividual, "caloriesPer100gram", recipe.getCaloriesPer100gram());
        dataPropertyService.addDataProperty(recipeIndividual, "fatContent", recipe.getFatContent());
        dataPropertyService.addDataProperty(recipeIndividual, "proteinContent", recipe.getProteinContent());
        dataPropertyService.addDataProperty(recipeIndividual, "sugarContent", recipe.getSugarContent());

        // Add object property assertions for ingredients
        objectPropertyService.addObjectProperties(recipeIndividual, "hasIngredient", recipe.getIngredients());

        ontologyService.saveOntology();
    }

    public void editRecipe(Recipe recipe) {
        OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + recipe.getId()));

        // Remove existing data properties
        dataPropertyService.removeDataProperties(recipeIndividual, "cookingTime");
        dataPropertyService.removeDataProperties(recipeIndividual, "preparationTime");
        dataPropertyService.removeDataProperties(recipeIndividual, "servingSize");
        dataPropertyService.removeDataProperties(recipeIndividual, "recipeName");
        dataPropertyService.removeDataProperties(recipeIndividual, "description");
        dataPropertyService.removeDataProperties(recipeIndividual, "caloriesPer100gram");
        dataPropertyService.removeDataProperties(recipeIndividual, "fatContent");
        dataPropertyService.removeDataProperties(recipeIndividual, "proteinContent");
        dataPropertyService.removeDataProperties(recipeIndividual, "sugarContent");

        // Add new data properties
        dataPropertyService.addDataProperty(recipeIndividual, "recipeName", recipe.getRecipeName());
        dataPropertyService.addDataProperty(recipeIndividual, "cookingTime", recipe.getCookingTime());
        dataPropertyService.addDataProperty(recipeIndividual, "preparationTime", recipe.getPreparationTime());
        dataPropertyService.addDataProperty(recipeIndividual, "servingSize", recipe.getServingSize());
        dataPropertyService.addDataProperty(recipeIndividual, "description", recipe.getDescription());

        // Calculate and save nutrition
        calculateNutritionAndAllergens(recipe, recipe.getIngredients());
        dataPropertyService.addDataProperty(recipeIndividual, "caloriesPer100gram", recipe.getCaloriesPer100gram());
        dataPropertyService.addDataProperty(recipeIndividual, "fatContent", recipe.getFatContent());
        dataPropertyService.addDataProperty(recipeIndividual, "proteinContent", recipe.getProteinContent());
        dataPropertyService.addDataProperty(recipeIndividual, "sugarContent", recipe.getSugarContent());

        // Remove existing object properties
        objectPropertyService.removeObjectProperties(recipeIndividual, "hasIngredient");

        // Add new object properties
        objectPropertyService.addObjectProperties(recipeIndividual, "hasIngredient", recipe.getIngredients());

        ontologyService.saveOntology();
    }

    public void removeRecipe(String recipeId) {
        OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + recipeId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        recipeIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }

    public List<Recipe> getRecipesByPreferences(List<String> preferences) {
        List<Recipe> recipes = new ArrayList<>();
        OWLClass recipeClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "Recipe"));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        List<OWLClass> preferenceClasses = preferences.stream()
                .map(pref -> dataFactory.getOWLClass(IRI.create(ontologyIRIStr + pref)))
                .collect(Collectors.toList());

        for (OWLNamedIndividual individual : individuals) {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(recipeClass))) {

                // Check if the individual matches all preference classes
                boolean matchesAllPreferences = preferenceClasses.stream()
                        .allMatch(preferenceClass -> ontologyService.getReasoner().getTypes(individual, false).containsEntity(preferenceClass));

                if (matchesAllPreferences) {
                    getRecipe(recipes, individual);
                }
            }
        }
        return recipes;
    }
}
