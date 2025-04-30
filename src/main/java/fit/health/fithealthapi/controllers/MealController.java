package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.MealDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.repository.FoodItemRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.repository.RecipeRepository;
import fit.health.fithealthapi.services.MealService;
import fit.health.fithealthapi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {
    private final MealService mealService;
    private final UserService userService;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByUsername(username);
    }

    @PostMapping
    public ResponseEntity<?> createMeal(@RequestBody MealDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        Meal saved = mealService.createMealFromDto(dto, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Meal>> getUserMeals() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(mealService.getUserMeals(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMeal(@PathVariable Long id) {
        return mealService.getMealById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeal(@PathVariable Long id, @RequestBody MealDto dto) {

        User user = getAuthenticatedUser();
        return mealService.getMealById(id)
                .map(existingMeal -> {
                    if (!existingMeal.getOwner().equals(user))
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
                    return ResponseEntity.ok(mealService.updateMealFromDto(dto,id,user));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal not found"));
    }

    @PutMapping("{id}/meal-item")
    public ResponseEntity<?> assignMealItem(@PathVariable Long id, @RequestBody MealItem mealItem) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeal(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return mealService.getMealById(id)
                .map(meal -> {
                    if (!meal.getOwner().equals(user))
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

                    mealService.deleteMeal(id);
                    return ResponseEntity.ok("Meal deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal not found"));
    }

    @PostMapping("/search")
    public ResponseEntity<List<Meal>> searchMeals(@RequestBody MealSearchDto searchDto) {
        return ResponseEntity.ok(mealService.searchMeals(searchDto));
    }

    @DeleteMapping("/meal-item/{id}")
    public ResponseEntity<?> deleteMealItem(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(mealService.removeMealItem(id,user));
    }
}
