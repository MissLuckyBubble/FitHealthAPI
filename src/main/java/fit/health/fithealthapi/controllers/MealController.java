package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.mappers.MealMapper;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.MealDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.dto.meal.MealSummaryDTO;
import fit.health.fithealthapi.services.MealService;
import fit.health.fithealthapi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

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
        MealSummaryDTO dto = mealService.getMealById(id);
        if(dto != null) return ResponseEntity.ok(dto);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @GetMapping("/meal-items/{id}")
    public ResponseEntity<?> getMealItem(@PathVariable Long id) {
        return mealService.getMealItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeal(@PathVariable Long id, @RequestBody MealDto dto) {
        User user = getAuthenticatedUser();
        ResponseEntity<String> responseEntity = getStringResponseEntity(id);
        if (responseEntity != null) return responseEntity;
        return ResponseEntity.ok(mealService.updateMealFromDto(dto,id,user));
    }

    @PutMapping("{id}/meal-item")
    public ResponseEntity<?> assignMealItem(@PathVariable Long id, @RequestBody MealItem mealItem) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeal(@PathVariable Long id) {
        ResponseEntity<String> responseEntity = getStringResponseEntity(id);
        if (responseEntity != null) return responseEntity;
        mealService.deleteMeal(id);
        return ResponseEntity.ok("Meal deleted successfully");
    }

    private ResponseEntity<String> getStringResponseEntity(Long id) {
        User user = getAuthenticatedUser();
        MealSummaryDTO existingMeal = mealService.getMealById(id);
        if (existingMeal == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal not found");
        }
        if (!Objects.equals(existingMeal.getOwner().getId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        return null;
    }


    @PostMapping("/search")
    public ResponseEntity<List<MealSummaryDTO>> searchMeals(@RequestBody MealSearchDto searchDto) {
        return ResponseEntity.ok(mealService.searchMeals(searchDto).stream().map(MealMapper::toMealSummaryDto).toList());
    }

    @DeleteMapping("/meal-item/{id}")
    public ResponseEntity<?> deleteMealItem(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(mealService.removeMealItem(id,user));
    }
}
