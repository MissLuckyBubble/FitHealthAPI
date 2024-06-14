package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.agents.MealPlanAgent;
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

    @PostMapping("/generate")
    public ResponseEntity<String> generateMealPlan(@RequestParam String username, @RequestParam int numberOfMeals, @RequestParam int days) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            List<Recipe> recipes = recipeService.getRecipesByPreferences(user.getDietaryPreferences());

            System.out.println(recipes.size());
            for (String recipe : user.getDietaryPreferences()
                 ) {
                System.out.println(recipe);
            }
            if (recipes.isEmpty()){
                return ResponseEntity.status(404).body("Recipes not found");
            }
            MealPlanAgent agent = new MealPlanAgent();
            agent.setUser(user);
            agent.setRecipes(recipes);
            agent.setUserService(userService);
            agent.setRecipeService(recipeService);
            agent.setNumberOfMeals(numberOfMeals);
            agent.setNumberOfDays(days);

            AgentController agentController = agentContainer.acceptNewAgent("mealPlanAgent" + username, agent);
            agentController.start();

            return ResponseEntity.ok("Meal plan generation started for user: " + username);
        } catch (StaleProxyException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to start meal plan agent");
        }
    }
}
