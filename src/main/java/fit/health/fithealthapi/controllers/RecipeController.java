package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.exceptions.IngredientNotFoundException;
import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.RecipeSearchRequest;
import fit.health.fithealthapi.model.enums.*;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.SharedService;
import fit.health.fithealthapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;
    @Autowired
    private UserService userService;
    @Autowired
    private SharedService sharedService;

    /**
     * Create a new recipe.
     */
    @PostMapping
    public ResponseEntity<?> createRecipe(@RequestBody Recipe recipe) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User owner = userService.getUserByUsername(username);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");
        }

        recipe.setOwner(owner);
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
    public ResponseEntity<?> updateRecipe(@PathVariable Long id, @RequestBody Recipe updatedRecipe, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(recipeService.getRecipeById(id).getOwner().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to update this meal plan.");
            }
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
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(recipeService.getRecipeById(id).getOwner().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this meal plan.");
            }
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

        searchRequest.setConditionSuitability(sharedService.convertToHealthConditionSuitability(searchRequest.getHealthConditions()));
        List<Recipe> recipes = recipeService.searchRecipes(
              searchRequest
        );
        return ResponseEntity.ok(recipes);
    }

}