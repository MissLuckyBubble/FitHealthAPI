package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.MealComponent;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.MealComponentDto;
import fit.health.fithealthapi.model.dto.MealComponentSearchRequest;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.repository.MealComponentRepository;
import fit.health.fithealthapi.services.*;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("/meal-components")
@AllArgsConstructor
@RestController
public class MealComponentController {

    private final MealComponentService mealComponentService;
    private final MealComponentSearchService mealComponentSearchService;
    private final UserService userService;
    private final MealComponentRepository mealComponentRepository;
    private final RecipeService recipeService;
    private final FoodItemService foodItemService;

    @GetMapping()
    public ResponseEntity<List<MealComponentDto>> getAllMealComponents(){
        return ResponseEntity.ok(mealComponentService.getAllComponents());
    }

    @PostMapping("/search")
    public ResponseEntity<?> getWithFilters(@RequestBody MealComponentSearchRequest searchRequest){
        return ResponseEntity.ok(mealComponentSearchService.searchMealComponents(searchRequest));
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication){
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            Optional<MealComponent> optionalMealComponent = mealComponentRepository.findByIdWithItems(id);
            if(optionalMealComponent.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            MealComponent mealComponent = optionalMealComponent.get();
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(mealComponent.getOwner().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this meal plan.");
            }

            if (mealComponent instanceof Recipe recipe) {
                recipeService.deleteRecipe(recipe.getId());
            } else if (mealComponent instanceof FoodItem foodItem) {
                foodItemService.deleteFoodItem(foodItem.getId());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown component type.");
            }
            return ResponseEntity.noContent().build();
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Transactional
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        return  ResponseEntity.ok(mealComponentRepository.findByIdWithItems(id));
    }
}
