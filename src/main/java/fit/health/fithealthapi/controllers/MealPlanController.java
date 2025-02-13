package fit.health.fithealthapi.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.MealPlanRequest;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.MealPlanService;
import fit.health.fithealthapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/meal-plans")
public class MealPlanController {

    @Autowired
    private MealPlanService mealPlanService;
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createMealPlan(@RequestBody MealPlanRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");
            }
            MealPlan mealPlan = mealPlanService.createMealPlan(request.getName(), user, request.getRecipeIds());
            return ResponseEntity.ok(mealPlan);
        }
        catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMealPlan(
            @PathVariable Long id,
            @RequestBody MealPlanRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername);

        MealPlan existingPlan = mealPlanService.getMealPlanById(id);
        if (existingPlan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Meal plan not found");
        }

        // Ensure either admin or owner
        if (!currentUser.getRole().equals(Role.ADMIN) &&
                !currentUser.getId().equals(existingPlan.getUser().getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to edit this meal plan.");
        }

        // Let the service update the meal plan fields
        MealPlan updatedMealPlan = mealPlanService.updateMealPlan(
                existingPlan,
                request.getName(),
                request.getRecipeIds()
                // pass other fields if you want to update them
        );

        return ResponseEntity.ok(updatedMealPlan);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMealPlan(@PathVariable Long id, Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername);
        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(mealPlanService.getMealPlanById(id).getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this meal plan.");
        }
        mealPlanService.deleteMealPlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealPlan> getMealPlanById(@PathVariable Long id) {
        MealPlan mealPlan = mealPlanService.getMealPlanById(id);
        return ResponseEntity.ok(mealPlan);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<MealPlan>> getMealPlansForUser(@PathVariable Long userId) {
        Set<MealPlan> mealPlans = mealPlanService.getMealPlansForUser(userId);
        return ResponseEntity.ok(mealPlans);
    }

    @GetMapping
    public ResponseEntity<List<MealPlan>> getMealPlans(
            @RequestParam(value = "filter", defaultValue = "{}") String filterJson,
            @RequestParam(value = "range", defaultValue = "[0,9]") String rangeJson,
            @RequestParam(value = "sort", defaultValue = "[\"id\",\"ASC\"]") String sortJson
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> filterMap = mapper.readValue(filterJson, new TypeReference<>() {});

            List<Integer> rangeList = mapper.readValue(rangeJson, new TypeReference<>() {});
            int start = rangeList.get(0);
            int end = rangeList.get(1);
            List<String> sortList = mapper.readValue(sortJson, new TypeReference<>() {});
            String sortField = sortList.get(0);
            String sortOrder = sortList.get(1);
            List<MealPlan> allMealPlans = mealPlanService.findMealPlans(filterMap, sortField, sortOrder);
            int total = allMealPlans.size();
            int safeEnd = Math.min(end, total - 1);
            if (start > safeEnd) {
                start = 0;
                safeEnd = -1;
            }
            List<MealPlan> paginated = safeEnd >= start
                    ? allMealPlans.subList(start, safeEnd + 1)
                    : List.of();

            String contentRange = String.format("%d-%d/%d", start, safeEnd, total);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", contentRange);

            return new ResponseEntity<>(paginated, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

}
