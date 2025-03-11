package fit.health.fithealthapi.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.exceptions.IngredientNotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.SearchRequest;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.FoodItemService;
import fit.health.fithealthapi.services.SharedService;
import fit.health.fithealthapi.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/food-items")
public class FoodItemController {

    @Autowired
    private FoodItemService foodService;
    @Autowired
    private UserService userService;
    @Autowired
    private SharedService sharedService;
    /**
     * Create a new FoodItem.
     *
     *
     * @param foodItem The FoodItem object to create.
     * @return The created FoodItem with dietary preferences populated.
     */
    @PostMapping
    public ResponseEntity<?> createFoodItem(@RequestBody FoodItem foodItem) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User owner = userService.getUserByUsername(username);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");
        }
        foodItem.setOwner(owner);

        FoodItem savedItem = foodService.saveFoodItem(foodItem);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    /**
     * Get all FoodItems.
     *
     * @return List of all FoodItems.
     */
    @GetMapping
    public ResponseEntity<?> getAllFoodItems(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "range", required = false) String range,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        try {
            // Parse filters
            Map<String, String> filters = filter != null
                    ? new ObjectMapper().readValue(filter, new TypeReference<Map<String, String>>() {})
                    : Collections.emptyMap();

            // Parse range
            int start = 0, end = 10;
            if (range != null) {
                int[] rangeArray = new ObjectMapper().readValue(range, int[].class);
                start = rangeArray[0];
                end = rangeArray[1] + 1;
            }

            // Parse sort
            String sortField = "id";
            String sortOrder = "ASC";
            if (sort != null) {
                String[] sortArray = new ObjectMapper().readValue(sort, String[].class);
                sortField = sortArray[0];
                sortOrder = sortArray[1];
            }

            // Fetch food items with filters, sorting, and pagination
            List<FoodItem> foodItems = foodService.getAllWithFilters(filters, sortField, sortOrder, start, end);

            // Total count for pagination
            long total = foodService.getTotalCount(filters);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "foodItems " + start + "-" + (end - 1) + "/" + total);

            return ResponseEntity.ok().headers(headers).body(foodItems);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Get a FoodItem by ID.
     *
     * @param id The ID of the FoodItem to retrieve.
     * @return The requested FoodItem.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFoodItemById(@PathVariable Long id) {
        try {
            FoodItem food = foodService.findById(id);
            return ResponseEntity.ok(food);
        } catch (IngredientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Update an existing FoodItem.
     *
     * @param id The ID of the FoodItem to update.
     * @param updatedFoodItem The updated FoodItem object.
     * @return The updated FoodItem.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFoodItem(
            @PathVariable Long id,
            @RequestBody FoodItem updatedFoodItem, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            FoodItem potentialFood = foodService.findById(id);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(potentialFood.getOwner().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to edit this food item.");
            }
            FoodItem savedItem = foodService.updateFoodItem(id,updatedFoodItem);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Delete a FoodItem by ID.
     *
     * @param id The ID of the FoodItem to delete.
     * @return ResponseEntity indicating the result of the operation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFoodItem(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(foodService.findById(id).getOwner().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to edit this food item.");
            }
            foodService.deleteFoodItem(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Food item not found");
        }
    }

    @PostMapping("/search/by-preferences")
    public ResponseEntity<List<FoodItem>> getFoodItemsByPreferences(@RequestBody List<DietaryPreference> preferences) {
        List<FoodItem> matchingFoodItems = foodService.findFoodItemsByPreferences(preferences);
        return ResponseEntity.ok(matchingFoodItems);
    }

    @PostMapping("/search/without-allergens")
    public ResponseEntity<List<FoodItem>> getFoodItemsWithoutAllergens(@RequestBody List<Allergen> allergens) {
        List<FoodItem> matchingFoodItems = foodService.findFoodItemsWithoutAllergens(allergens);
        return ResponseEntity.ok(matchingFoodItems);
    }

    @PostMapping("/search/health-condition")
    public ResponseEntity<List<FoodItem>> getFoodItemsByHealthConditionSuitability (@RequestBody List<String> preferences) {
        List<HealthConditionSuitability> healthConditionSuitabilities = sharedService.convertToHealthConditionSuitability(preferences);
        List<FoodItem> matchingFoodItems = foodService.findFoodItemsByHealthConditions(healthConditionSuitabilities);
        return ResponseEntity.ok(matchingFoodItems);
    }

    @PostMapping("/search")
    public ResponseEntity<List<FoodItem>> searchFoodItems(@RequestBody SearchRequest searchRequest) {
        searchRequest.setHealthSuitabilities(sharedService.convertToHealthConditionSuitability(searchRequest.getHealthConditions()));

        List<FoodItem> matchingFoodItems = foodService.searchFoodItems(searchRequest);

        return ResponseEntity.ok(matchingFoodItems);
    }
}
