package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.MealPlanService;
import fit.health.fithealthapi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService mealPlanService;
    private final UserService userService;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByUsername(username);
    }

    @PostMapping
    public ResponseEntity<?> createMealPlan(@RequestBody MealPlan mealPlan) {
        User user = getAuthenticatedUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");

        mealPlan.setOwner(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanService.createMealPlan(mealPlan));
    }

    @GetMapping
    public ResponseEntity<List<MealPlan>> getOwnerMealPlans() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(mealPlanService.getOwnerMealPlans(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMealPlan(@PathVariable Long id) {
        return mealPlanService.getMealPlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMealPlan(@PathVariable Long id, @RequestBody MealPlan mealPlan) {
        User user = getAuthenticatedUser();
        Optional<MealPlan> optionalMealPlan = mealPlanService.getMealPlanById(id);
        if(optionalMealPlan.isPresent()) {
            MealPlan existingPlan = optionalMealPlan.get();
            if (Objects.equals(existingPlan.getOwner().getId(), user.getId()) || user.getRole() == Role.ADMIN){
                mealPlan.setId(id);
                mealPlan.setOwner(user);
                return ResponseEntity.ok(mealPlanService.createMealPlan(mealPlan));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        else return (ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal plan not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMealPlan(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return mealPlanService.getMealPlanById(id)
                .map(mealPlan -> {
                    if (!mealPlan.getOwner().equals(user))
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

                    mealPlanService.deleteMealPlan(id);
                    return ResponseEntity.ok("Meal plan deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal plan not found"));
    }

    @PutMapping("/assign-meal")
    public ResponseEntity<?> assignMealToMealPlan(@RequestBody CreateMealRequestDto assignMealDto) {
        mealPlanService.setMeal(assignMealDto);
        return ResponseEntity.ok("Meal assigned to meal plan successfully");
    }

    @PutMapping("{id}/meal/{recipeType}")
    public ResponseEntity<?> removeMeal(@PathVariable Long id, @PathVariable String recipeType) {
        User user = getAuthenticatedUser();
        MealPlan mealPlan = mealPlanService.removeMeal(id, RecipeType.fromString(recipeType),user);
        return ResponseEntity.ok(mealPlan);
    }

    @PutMapping("/search")
    public ResponseEntity<?> searchMealPlans(@RequestBody MealSearchDto searchDto) {
        return ResponseEntity.ok(mealPlanService.searchMealPlan(searchDto));
    }
}
