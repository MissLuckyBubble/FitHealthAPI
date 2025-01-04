package fit.health.fithealthapi.services;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.repository.FoodItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FoodItemService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private SharedService sharedService;


    public Optional<FoodItem> saveFoodItem(FoodItem foodItem) {
        Optional<FoodItem> existingItem = foodItemRepository.findByName(foodItem.getName());

        if (existingItem.isPresent()) {
            return Optional.empty();
        }

        ontologyService.createItemType(foodItem.getName(), "FoodItem");

        addDataProperties(foodItem);

        inferDietaryPreferences(foodItem);

        return Optional.of(foodItemRepository.save(foodItem));
    }

    private void inferDietaryPreferences(FoodItem foodItem) {
        Set<String> superClasses = ontologyService.getSuperClasses(foodItem.getName());
        Set<DietaryPreference> dietaryPreferences = superClasses.stream()
                .filter(ontologyService::isDietaryPreference) // Use reusable method
                .map(String::toUpperCase)
                .map(DietaryPreference::valueOf)
                .collect(Collectors.toSet());
        foodItem.setDietaryPreferences(dietaryPreferences);
    }

    private void addDataProperties(FoodItem foodItem) {
        ontologyService.addDataPropertyRestriction(foodItem.getName(), "caloriesPer100gram", foodItem.getCaloriesPer100g());
        ontologyService.addDataPropertyRestriction(foodItem.getName(), "proteinContent", foodItem.getProteinContent());
        ontologyService.addDataPropertyRestriction(foodItem.getName(), "fatContent", foodItem.getFatContent());
        ontologyService.addDataPropertyRestriction(foodItem.getName(), "sugarContent", foodItem.getSugarContent());

        if (foodItem.getAllergens() != null && !foodItem.getAllergens().isEmpty()) {
            for (Allergen allergen : foodItem.getAllergens()) {
                ontologyService.addObjectPropertyRestriction(
                        foodItem.getName(),
                        "hasAllergen",
                        sharedService.convertToPascalCase(allergen.name())
                );
            }
        } else {
            ontologyService.addObjectPropertyRestriction(foodItem.getName(), "hasAllergen", "Allergen_Free");
            foodItem.setAllergens(Set.of(Allergen.ALLERGEN_FREE));
        }

        ontologyService.saveOntology();
        ontologyService.getReasoner().flush();
        ontologyService.convertToDefinedClass(foodItem.getName());

    }

    /**
     * Retrieve all FoodItems from the database.
     * @return List of all FoodItems.
     */
    public List<FoodItem> getAllFoodItems() {
        return foodItemRepository.findAll();
    }

    /**
     * Update an existing FoodItem in both the ontology and the database.
     * @param id The ID of the FoodItem to update.
     * @param updatedFoodItem The updated FoodItem object.
     * @return The updated FoodItem.
     */
    @Transactional
    public FoodItem updateFoodItem(Long id, FoodItem updatedFoodItem) {
        // Fetch the existing FoodItem
        FoodItem existingFoodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FoodItem not found"));
        String updatedName = updatedFoodItem.getName();

        if (!existingFoodItem.getName().equals(updatedName)) {
            ontologyService.renameItem(existingFoodItem.getName(), updatedName);
            existingFoodItem.setName(updatedName);
        }

        ontologyService.removeDefinedClass(updatedName);
        ontologyService.removeDataPropertyRestrictions(updatedName);
        ontologyService.removeObjectPropertyRestrictions(updatedName);


        addDataProperties(updatedFoodItem);

        inferDietaryPreferences(updatedFoodItem);

        existingFoodItem.setCaloriesPer100g(updatedFoodItem.getCaloriesPer100g());
        existingFoodItem.setFatContent(updatedFoodItem.getFatContent());
        existingFoodItem.setProteinContent(updatedFoodItem.getProteinContent());
        existingFoodItem.setSugarContent(updatedFoodItem.getSugarContent());
        existingFoodItem.setAllergens(new HashSet<>(updatedFoodItem.getAllergens()));
        existingFoodItem.setDietaryPreferences(new HashSet<>(updatedFoodItem.getDietaryPreferences()));

        return foodItemRepository.save(existingFoodItem);
    }



    /**
     * Delete a FoodItem from both the ontology and the database.
     * @param id The ID of the FoodItem to delete.
     */
    public void deleteFoodItem(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FoodItem not found"));

        ontologyService.deleteItem(foodItem.getName());

        foodItemRepository.deleteById(id);
    }

    public List<DietaryPreference> convertToDietaryPreferences(List<String> preferences) {
        List<DietaryPreference> dietaryPreferences = new ArrayList<>();
        for (String preference : preferences) {
            String normalizedPreference = preference.toUpperCase().replace(" ", "_");
            if(ontologyService.isDietaryPreference(normalizedPreference)){
                dietaryPreferences.add(DietaryPreference.valueOf(normalizedPreference));
            }
        }
        return dietaryPreferences;
    }

    public List<Allergen> convertToAllergens(List<String> allergens) {
        List<Allergen> allergenEnums = new ArrayList<>();
        for (String allergen : allergens) {
            String normalizedAllergen = allergen.toUpperCase().replace(" ", "_");
            if(ontologyService.isAllergen(normalizedAllergen)){
                allergenEnums.add(Allergen.valueOf(normalizedAllergen));
            }
        }
        return allergenEnums;
    }

    public List<FoodItem> findFoodItemsByPreferences(List<DietaryPreference> preferences) {
        List<FoodItem> allFoodItems = foodItemRepository.findAll();
        List<FoodItem> matchingFoodItems = new ArrayList<>();

        for (FoodItem foodItem : allFoodItems) {
            if (foodItem.getDietaryPreferences().containsAll(preferences)) {
                matchingFoodItems.add(foodItem);
            }
        }
        return matchingFoodItems;
    }

    public List<FoodItem> findFoodItemsWithoutAllergens(List<Allergen> allergens) {
        List<FoodItem> allFoodItems = foodItemRepository.findAll();
        List<FoodItem> matchingFoodItems = new ArrayList<>();

        for (FoodItem foodItem : allFoodItems) {
            if (Collections.disjoint(foodItem.getAllergens(), allergens)) {
                matchingFoodItems.add(foodItem);
            }
        }
        return matchingFoodItems;
    }
}
