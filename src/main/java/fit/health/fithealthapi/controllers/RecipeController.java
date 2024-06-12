package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.services.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @PostMapping("/create")
    public ResponseEntity<?> createRecipe(@RequestBody Recipe recipe) {
        try {
            recipeService.createRecipe(recipe);
            return ResponseEntity.ok("Recipe created successfully");
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<Recipe> getAllRecipes() {
        return recipeService.getRecipes();
    }

    @PutMapping("/edit")
    public ResponseEntity<?> editRecipe(@RequestBody Recipe recipe) {
        try {
            recipeService.editRecipe(recipe);
            return ResponseEntity.ok("Recipe edited successfully");
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRecipe(@RequestParam String recipeId) {
        recipeService.removeRecipe(recipeId);
        return ResponseEntity.ok("Recipe deleted successfully");
    }

    @GetMapping("/search")
    public List<Recipe> getRecipesByPreferences(@RequestParam List<String> preferences) {
        return recipeService.getRecipesByPreferences(preferences);
    }
}
