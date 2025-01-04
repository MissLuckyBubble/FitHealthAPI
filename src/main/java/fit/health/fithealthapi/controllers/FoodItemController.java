package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.services.FoodItemService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/food-items")
public class FoodItemController {

    @Autowired
    private FoodItemService foodService;
    /**
     * Create a new FoodItem.
     *
     *
     * @param foodItem The FoodItem object to create.
     * @return The created FoodItem with dietary preferences populated.
     */
    @PostMapping
    public ResponseEntity<?> createFoodItem(@RequestBody FoodItem foodItem) {
        Optional<FoodItem> savedItem = foodService.saveFoodItem(foodItem);

        if (savedItem.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Food item already exists",
                            "linkedOntologyClass", foodItem.getName()
                    ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem.get());
    }

    /**
     * Get all FoodItems.
     *
     * @return List of all FoodItems.
     */
    @GetMapping
    public ResponseEntity<List<FoodItem>> getAllFoodItems() {
        List<FoodItem> foodItems = foodService.getAllFoodItems();
        return ResponseEntity.ok(foodItems);
    }

    /**
     * Get a FoodItem by ID.
     *
     * @param id The ID of the FoodItem to retrieve.
     * @return The requested FoodItem.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FoodItem> getFoodItemById(@PathVariable Long id) {
        return foodService.getAllFoodItems().stream()
                .filter(foodItem -> foodItem.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing FoodItem.
     *
     * @param id The ID of the FoodItem to update.
     * @param updatedFoodItem The updated FoodItem object.
     * @return The updated FoodItem.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FoodItem> updateFoodItem(
            @PathVariable Long id,
            @RequestBody FoodItem updatedFoodItem) {
        try {
            FoodItem foodItem = foodService.updateFoodItem(id, updatedFoodItem);
            return ResponseEntity.ok(foodItem);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Delete a FoodItem by ID.
     *
     * @param id The ID of the FoodItem to delete.
     * @return ResponseEntity indicating the result of the operation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFoodItem(@PathVariable Long id) {
        try {
            foodService.deleteFoodItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/search/by-preferences")
    public ResponseEntity<List<FoodItem>> getFoodItemsByPreferences(@RequestBody List<String> preferences) {
        List<DietaryPreference> dietaryPreferences = foodService.convertToDietaryPreferences(preferences);
        List<FoodItem> matchingFoodItems = foodService.findFoodItemsByPreferences(dietaryPreferences);
        return ResponseEntity.ok(matchingFoodItems);
    }

    @PostMapping("/search/without-allergens")
    public ResponseEntity<List<FoodItem>> getFoodItemsWithoutAllergens(@RequestBody List<String> allergens) {
        List<Allergen> allergenEnums = foodService.convertToAllergens(allergens);
        List<FoodItem> matchingFoodItems = foodService.findFoodItemsWithoutAllergens(allergenEnums);
        return ResponseEntity.ok(matchingFoodItems);
    }
}
