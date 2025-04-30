package fit.health.fithealthapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.agents.UserAgent;
import fit.health.fithealthapi.mappers.MealPlanMapper;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealPlanRequestDTO;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanSummaryDTO;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.DiaryEntryService;
import fit.health.fithealthapi.services.MealPlanService;
import fit.health.fithealthapi.services.UserService;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService mealPlanService;
    private final UserService userService;
    private final DiaryEntryService diaryEntryService;
    private final AgentContainer agentContainer;

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
    public ResponseEntity<List<MealPlanSummaryDTO>> getOwnerMealPlans() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(mealPlanService.getOwnerMealPlans(user).stream().map(MealPlanMapper::toSummaryDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMealPlan(@PathVariable Long id) {
        Optional<MealPlan> optionalMealPlan = mealPlanService.getMealPlanById(id);
        if (optionalMealPlan.isPresent()) {
            return ResponseEntity.ok(MealPlanMapper.toDetailsDTO(optionalMealPlan.get()));
        }
        else{
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No such meal plan");
        }
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

    @PostMapping("/search")
    public ResponseEntity<?> searchMealPlans(@RequestBody MealSearchDto searchDto) {
        return ResponseEntity.ok(mealPlanService.searchMealPlan(searchDto).stream().map(MealPlanMapper::toSummaryDTO));
    }


    @PostMapping("{id}/copy-to-diary/{date}")
    public ResponseEntity<?> copyMealPlanToDiary(
            @PathVariable LocalDate date, @PathVariable Long id) {
        User user = getAuthenticatedUser();
        diaryEntryService.copyMealPlanToDiary(id, date, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/debug-meal-plan/{id}")
    public ResponseEntity<?> debug(@PathVariable Long id) {
        MealPlan mealPlan = mealPlanService.getMealPlanById(id).orElseThrow();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValueAsString(mealPlan); // 🔥 will throw if problem
            return ResponseEntity.ok(mealPlan);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Manual serialization failed: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateMealPlan(@RequestBody MealPlanRequestDTO request) {
        User user = getAuthenticatedUser(); // Your existing auth logic
            try {
                UserAgent userAgent = new UserAgent();
                userAgent.init(mealPlanService,user,request.getRecipeTypes(),request.getDays());
                AgentController userAgentController = agentContainer.acceptNewAgent("userAgent" + user.getUsername() + UUID.randomUUID(), userAgent);
                userAgentController.start();

                return ResponseEntity.ok("Meal plan request sent to JADE agents.");
            } catch (StaleProxyException e) {
                return ResponseEntity.status(500).body("Failed to start agents");
            }
    }
}
