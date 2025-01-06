package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.exceptions.IngredientNotFoundException;
import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.dto.RecipeSearchRequest;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.SharedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;
    @Autowired
    private SharedService sharedService;

    /**
     * Create a new recipe.
     */
    @PostMapping
    public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe) {
        Recipe createdRecipe = recipeService.saveRecipe(recipe);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    /**
     * Get all recipes.
     */
    @GetMapping
    public ResponseEntity<List<Recipe>> getAllRecipes() {
        List<Recipe> recipes = recipeService.getAllRecipes();
        return ResponseEntity.ok(recipes);
    }

    /**
     * Get a recipe by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id) {
        try {
            Recipe recipe = recipeService.getRecipeById(id);
            return ResponseEntity.ok(recipe);
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Update an existing recipe.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe updatedRecipe) {
        try {
            Recipe recipe = recipeService.updateRecipe(id, updatedRecipe);
            return ResponseEntity.ok(recipe);
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IngredientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Delete a recipe by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        try {
            recipeService.deleteRecipe(id);
            return ResponseEntity.noContent().build();
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<Recipe>> searchRecipes(
            @RequestBody RecipeSearchRequest searchRequest
    ) {
        List<DietaryPreference> dietaryPreferences = sharedService.convertToDietaryPreferences(searchRequest.getDietaryPreferences());
        List<Allergen> allergens = sharedService.convertToAllergens(searchRequest.getAllergens());
        List<HealthConditionSuitability> healthConditions = sharedService.convertToHealthConditionSuitability(searchRequest.getHealthConditions());

        List<Recipe> recipes = recipeService.searchRecipes(
                dietaryPreferences,
                allergens,
                healthConditions,
                searchRequest.getIngredientNames(),
                searchRequest.getMinCalories(),
                searchRequest.getMaxCalories(),
                searchRequest.getMaxTotalTime(),
                searchRequest.getName()
        );
        return ResponseEntity.ok(recipes);
    }

}