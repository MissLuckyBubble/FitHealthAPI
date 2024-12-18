package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.agents.MealPlanAgent;
import fit.health.fithealthapi.agents.UserAgent;
import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mealplans")
public class MealPlanController {

    @Autowired
    private AgentContainer agentContainer;

    @Autowired
    private UserService userService;

    @Autowired
    private RecipeService recipeService;


    @GetMapping("")
    public ResponseEntity<?> getMealPlans(@RequestParam String username) {
        try {
            List<MealPlan> mealPlans = userService.getMealPlans(username);
            if (mealPlans == null || mealPlans.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(mealPlans);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMealPlan(@PathVariable("id") String mealPlanIdFragment) {
        try {
            userService.deleteMealPlan(mealPlanIdFragment);
            return ResponseEntity.ok("Meal plan deleted successfully.");
        } catch (CustomException e) {
            return ResponseEntity.status(404).body("Meal plan not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateMealPlan(@RequestParam String username, @RequestParam int numberOfMeals, @RequestParam int days) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            List<Recipe> recipes = recipeService.getRecipesByPreferences(user.getDietaryPreferences());
            if (recipes.isEmpty()) {
                return ResponseEntity.status(404).body("Recipes not found");
            }

            MealPlanAgent mealPlanAgent = new MealPlanAgent();
            mealPlanAgent.init(userService, recipeService, user, recipes, numberOfMeals, days);
            AgentController mealPlanAgentController = agentContainer.acceptNewAgent("mealPlanAgent" + username, mealPlanAgent);
            mealPlanAgentController.start();

            UserAgent userAgent = new UserAgent();
            userAgent.init(userService, recipeService, user, recipes, numberOfMeals, days);
            AgentController userAgentController = agentContainer.acceptNewAgent("userAgent" + username, userAgent);
            userAgentController.start();

            return ResponseEntity.ok("Meal plan generation started for user: " + username);
        } catch (StaleProxyException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to start agents");
        }
    }
}
