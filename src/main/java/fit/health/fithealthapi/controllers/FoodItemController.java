package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.services.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/food/items")
public class FoodItemController {

    @Autowired
    private OntologyService ontologyService;

    @GetMapping
    public ResponseEntity<List<FoodItem>> getFoodItems() {
        List<FoodItem> foodItems = ontologyService.getFoodItems();
        return ResponseEntity.ok(foodItems);
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<String>> getDietaryPreferences() {
        List<String> dietaryPreferences = ontologyService.getDietaryPreferences();
        return ResponseEntity.ok(dietaryPreferences);
    }

    @GetMapping("/by-preference")
    public ResponseEntity<List<FoodItem>> getFoodItemsByPreference(@RequestParam String preference) {
        List<FoodItem> foodItems = ontologyService.getFoodItemsByPreference(preference);
        return ResponseEntity.ok(foodItems);
    }

    @PostMapping("/by-preferences")
    public ResponseEntity<List<FoodItem>> getFoodItemsByPreferences(@RequestBody List<String> preferences) {
        List<FoodItem> foodItems = ontologyService.getFoodItemsByPreferences(preferences);
        return ResponseEntity.ok(foodItems);
    }


    @PostMapping
    public ResponseEntity<String> createFoodItem(@RequestBody FoodItem foodItem) {
        ontologyService.createFoodItem(foodItem);
        return ResponseEntity.ok("Food item created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateFoodItem(@PathVariable String id, @RequestBody FoodItem foodItem) {
        foodItem.setId(id);
        ontologyService.editFoodItem(foodItem);
        return ResponseEntity.ok("Food item updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFoodItem(@PathVariable String id) {
        ontologyService.removeFoodItem(id);
        return ResponseEntity.ok("Food item deleted successfully");
    }
}