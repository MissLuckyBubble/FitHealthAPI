package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.EditUserDTO;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private RecipeService recipeService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext(); // Clears the user's security context
        return ResponseEntity.ok("Logged out successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody EditUserDTO user, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to edit this user.");
            }
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this user.");
            }
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/favorite-recipes/{recipeId}")
    public ResponseEntity<?> addFavoriteRecipe(@PathVariable Long userId, @PathVariable Long recipeId) {
        try {
            Recipe recipe = recipeService.getRecipeById(recipeId);

            if (userService.addFavoriteRecipe(userId, recipe)) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body("Recipe with ID " + recipeId + " successfully added to user with ID " + userId + "'s favorites.");
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Recipe is already in the user's favorites");
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipe not found.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/favorite-recipes/{recipeId}")
    public ResponseEntity<?> removeFavoriteRecipe(@PathVariable Long userId, @PathVariable Long recipeId) {
        try {
            Recipe recipe = recipeService.getRecipeById(recipeId);

            if (userService.removeFavoriteRecipe(userId, recipe)) {
                return ResponseEntity.ok("Recipe with ID " + recipeId + " successfully removed from user with ID " + userId + "'s favorites.");
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Recipe is not in the user's favorites.");
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipe not found.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
