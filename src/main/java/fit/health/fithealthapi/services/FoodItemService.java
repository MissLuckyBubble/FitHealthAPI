package fit.health.fithealthapi.services;
import fit.health.fithealthapi.exceptions.IngredientNotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.RecipeIngredient;
import fit.health.fithealthapi.model.dto.InferredPreferences;
import fit.health.fithealthapi.model.dto.SearchRequest;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.repository.FoodItemRepository;
import fit.health.fithealthapi.repository.RecipeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final RecipeRepository recipeRepository;
    private final OntologyService ontologyService;
    private final EntityManager entityManager;
    private final SharedService sharedService;

    public FoodItemService(FoodItemRepository foodItemRepository, RecipeRepository recipeRepository, OntologyService ontologyService, EntityManager entityManager, SharedService sharedService) {
        this.foodItemRepository = foodItemRepository;
        this.recipeRepository = recipeRepository;
        this.ontologyService = ontologyService;
        this.entityManager = entityManager;
        this.sharedService = sharedService;
    }

    // ===================== Food Item Operations =====================

    public FoodItem saveFoodItem(FoodItem foodItem) {
        String ontologyLinkedName = sharedService.convertToOntoCase(foodItem.getName() + new SimpleDateFormat("HHmmss").format(new java.util.Date()));
        foodItem.setOntologyLinkedName(ontologyLinkedName);
        addDataProperties(foodItem);
        inferPreferences(foodItem);
        updateRecipes(foodItem);
        return foodItemRepository.save(foodItem);
    }

    @Transactional(readOnly = true)
    public FoodItem getById(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException("Food item not found with ID: " + id));
    }
    /**
     * Update an existing FoodItem in both the ontology and the database.
     * @param id The ID of the FoodItem to update.
     * @param updatedFoodItem The updated EditFoodDTO object.
     * @return The updated FoodItem.
     */
    @Transactional
    public FoodItem updateFoodItem(Long id, FoodItem updatedFoodItem) {
        FoodItem existingFoodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FoodItem not found"));

        if(!ontologyFieldAreEqual(updatedFoodItem, existingFoodItem)){
            ontologyService.removeDefinedClass(updatedFoodItem.getOntologyLinkedName());
            ontologyService.removeDataPropertyRestrictions(updatedFoodItem.getOntologyLinkedName());
            ontologyService.removeObjectPropertyRestrictions(updatedFoodItem.getOntologyLinkedName());
            addDataProperties(updatedFoodItem);
            inferPreferences(updatedFoodItem);
            updateFoodFields(updatedFoodItem, existingFoodItem);
        }

        existingFoodItem.setName(updatedFoodItem.getName());
        existingFoodItem.setVerifiedByAdmin(updatedFoodItem.isVerifiedByAdmin());
        updateRecipes(existingFoodItem);
        return foodItemRepository.save(existingFoodItem);
    }

    private void updateRecipes(FoodItem foodItem){
        Set<RecipeIngredient> ingredients = foodItem.getRecipes();
        for (RecipeIngredient ingredient : ingredients) {
            Recipe recipe = ingredient.getRecipe();
            if (recipe != null) {
                recipe.checkAndUpdateVerification();
                recipeRepository.save(recipe);
            }
        }

    }

    private void updateFoodFields(FoodItem updatedFoodItem, FoodItem existingFoodItem) {
        existingFoodItem.getMacronutrients().reset();
        existingFoodItem.getMacronutrients().add(updatedFoodItem.getMacronutrients());
        existingFoodItem.setAllergens(new HashSet<>(updatedFoodItem.getAllergens()));
        existingFoodItem.setDietaryPreferences(new HashSet<>(updatedFoodItem.getDietaryPreferences()));
        existingFoodItem.setHealthConditionSuitabilities(new HashSet<>(updatedFoodItem.getHealthConditionSuitabilities()));
    }

    private boolean ontologyFieldAreEqual(FoodItem updatedFoodItem, FoodItem existingFoodItem) {
        if (updatedFoodItem == null || existingFoodItem == null) {
            return false;
        }

        // Compare macronutrient values
        Macronutrients updatedMacronutrients = updatedFoodItem.getMacronutrients();
        Macronutrients existingMacronutrients = existingFoodItem.getMacronutrients();

        if (updatedMacronutrients == null || existingMacronutrients == null) {
            return false;
        }

        return Objects.equals(updatedMacronutrients.getCalories(), existingMacronutrients.getCalories()) &&
                Objects.equals(updatedMacronutrients.getFat(), existingMacronutrients.getFat()) &&
                Objects.equals(updatedMacronutrients.getProtein(), existingMacronutrients.getProtein()) &&
                Objects.equals(updatedMacronutrients.getSugar(), existingMacronutrients.getSugar()) &&
                Objects.equals(updatedMacronutrients.getSalt(), existingMacronutrients.getSalt()) &&
                Objects.equals(updatedFoodItem.getAllergens(), existingFoodItem.getAllergens());
    }


    public List<FoodItem> getAllFoodItems() {
        return foodItemRepository.findAll();
    }

    public FoodItem findById(long id) {
        return foodItemRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    /**
     * Delete a FoodItem from both the ontology and the database.
     * @param id The ID of the FoodItem to delete.
     */
    public void deleteFoodItem(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FoodItem not found"));

        ontologyService.deleteItem(foodItem.getOntologyLinkedName());

        foodItemRepository.deleteById(id);
    }

    // ===================== Preference and Filtering Methods =====================


    public List<FoodItem> findFoodItemsByPreferences(List<DietaryPreference> preferences) {
        return filterFoodItemsByPreferences(foodItemRepository.findAll(), preferences);
    }

    public List<FoodItem> findFoodItemsWithoutAllergens(List<Allergen> allergens) {
        return filterFoodItemsWithoutAllergens(foodItemRepository.findAll(), allergens);
    }

    public List<FoodItem> findFoodItemsByHealthConditions(List<HealthConditionSuitability> preferences) {
        return filterFoodItemsByHealthConditions(foodItemRepository.findAll(), preferences);
    }
    public List<FoodItem> searchFoodItems(SearchRequest searchRequest) {
        List<FoodItem> allFoodItems = foodItemRepository.findAll();

        return allFoodItems.stream()
                .filter(foodItem -> foodItem.getDietaryPreferences().containsAll(searchRequest.getDietaryPreferences()))
                .filter(foodItem -> Collections.disjoint(foodItem.getAllergens(), searchRequest.getAllergens()))
                .filter(foodItem -> foodItem.getHealthConditionSuitabilities().containsAll(searchRequest.getHealthSuitabilities()))
                .collect(Collectors.toList());
    }

    public Optional<FoodItem> findByName(String name) {
        return foodItemRepository.findByName(name);
    }

    public List<FoodItem> getAllWithFilters(Map<String, ? extends Object> filters, String sortField, String sortOrder, int start, int end) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FoodItem> query = cb.createQuery(FoodItem.class);
        Root<FoodItem> foodItem = query.from(FoodItem.class);

        Predicate predicate = buildFoodItemPredicate(cb, foodItem, (Map<String, Object>) filters);
        query.where(predicate);

        if ("ASC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.asc(foodItem.get(sortField)));
        } else {
            query.orderBy(cb.desc(foodItem.get(sortField)));
        }

        TypedQuery<FoodItem> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(start);
        typedQuery.setMaxResults(end - start + 1);

        return typedQuery.getResultList();
    }

    public long getTotalCount(Map<String, Object> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<FoodItem> foodItem = query.from(FoodItem.class);

        Predicate predicate = buildFoodItemPredicate(cb, foodItem, filters);
        query.where(predicate);
        query.select(cb.count(foodItem));

        return entityManager.createQuery(query).getSingleResult();
    }



    // ===================== Helper Methods =====================

    private Predicate buildFoodItemPredicate(CriteriaBuilder cb, Root<FoodItem> foodItem, Map<String, Object> filters) {
        Predicate predicate = cb.conjunction();

        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Path<Object> field = foodItem.get(filter.getKey());

            if (filter.getValue() instanceof List<?> list) {
                predicate = cb.and(predicate, field.in(list));
            } else {
                predicate = cb.and(predicate, cb.equal(field, filter.getValue()));
            }
        }

        return predicate;
    }

    private void addDataProperties(FoodItem foodItem) {
        ontologyService.createItemType(foodItem.getOntologyLinkedName(), "FoodItem");
        ontologyService.addDataPropertyRestriction(foodItem.getOntologyLinkedName(), "caloriesPer100gram", foodItem.getMacronutrients().getCalories());
        ontologyService.addDataPropertyRestriction(foodItem.getOntologyLinkedName(), "proteinContent", foodItem.getMacronutrients().getProtein());
        ontologyService.addDataPropertyRestriction(foodItem.getOntologyLinkedName(), "fatContent", foodItem.getMacronutrients().getFat());
        ontologyService.addDataPropertyRestriction(foodItem.getOntologyLinkedName(), "sugarContent", foodItem.getMacronutrients().getSugar());
        ontologyService.addDataPropertyRestriction(foodItem.getOntologyLinkedName(), "saltContent", foodItem.getMacronutrients().getSalt());

        // Add allergens or default to "Allergen_Free"
        if (foodItem.getAllergens() != null && !foodItem.getAllergens().isEmpty()) {
            foodItem.getAllergens().forEach(allergen ->
                    ontologyService.addObjectPropertyRestriction(foodItem.getOntologyLinkedName(), "hasAllergen", allergen.toOntologyCase())
            );
        } else {
            ontologyService.addObjectPropertyRestriction(foodItem.getOntologyLinkedName(), "hasAllergen", "Allergen_Free");
            foodItem.setAllergens(Set.of(Allergen.ALLERGEN_FREE));
        }

        // Final ontology operations
        ontologyService.saveOntology();
        ontologyService.getReasoner().flush();
        ontologyService.convertToDefinedClass(foodItem.getOntologyLinkedName());
    }

    private void inferPreferences(FoodItem foodItem) {
        InferredPreferences inferredPreferences = ontologyService.inferPreferences(foodItem.getOntologyLinkedName());
        foodItem.setDietaryPreferences(inferredPreferences.getDietaryPreferences());
        foodItem.setHealthConditionSuitabilities(inferredPreferences.getHealthConditionSuitabilities());
    }

    // ===================== Filtering Methods =====================

    private List<FoodItem> filterFoodItemsByPreferences(List<FoodItem> foodItems, List<DietaryPreference> preferences) {
        return foodItems.stream()
                .filter(foodItem -> foodItem.getDietaryPreferences().containsAll(preferences))
                .collect(Collectors.toList());
    }

    private List<FoodItem> filterFoodItemsWithoutAllergens(List<FoodItem> foodItems, List<Allergen> allergens) {
        return foodItems.stream()
                .filter(foodItem -> Collections.disjoint(foodItem.getAllergens(), allergens))
                .collect(Collectors.toList());
    }

    private List<FoodItem> filterFoodItemsByHealthConditions(List<FoodItem> foodItems, List<HealthConditionSuitability> preferences) {
        return foodItems.stream()
                .filter(foodItem -> foodItem.getHealthConditionSuitabilities().containsAll(preferences))
                .collect(Collectors.toList());
    }
}