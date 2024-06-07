package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.services.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BasicController {

    @Autowired
    private OntologyService ontologyService;

    @GetMapping("/food-items")
    public ResponseEntity<List<FoodItem>> getFoodItems() {
        List<FoodItem> foodItems = ontologyService.getFoodItems();
        return ResponseEntity.ok(foodItems);
    }
    @GetMapping("/food-items/by-preference")
    public ResponseEntity<List<FoodItem>> getFoodItemsByPreference(@RequestParam String preference) {
        List<FoodItem> foodItems = ontologyService.getFoodItemsByPreference(preference);
        return ResponseEntity.ok(foodItems);
    }

    @GetMapping("/dietary-preferences")
    public ResponseEntity<List<String>> getDietaryPreferences() {
        List<String> dietaryPreferences = ontologyService.getDietaryPreferences();
        return ResponseEntity.ok(dietaryPreferences);
    }
}
