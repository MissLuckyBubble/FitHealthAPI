package fit.health.fithealthapi.agents;

import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class MealPlanAgent extends Agent {

    @Setter
    private UserService userService;
    @Setter
    private RecipeService recipeService;
    @Setter
    private User user;
    @Setter
    private List<Recipe> recipes;
    @Setter
    private int numberOfMeals;
    @Setter
    private int numberOfDays;

    public MealPlanAgent() {
        // Празен конструктор за JADE
    }


    @Override
    protected void setup() {
        addBehaviour(new GenerateMealPlanBehaviour(user.getUsername()));
    }

    private class GenerateMealPlanBehaviour extends OneShotBehaviour {
        private final String username;

        public GenerateMealPlanBehaviour(String username) {
            this.username = username;
        }

        @Override
        public void action() {
            List<Recipe> mealPlan = generateMealPlan(user, recipes);
            userService.saveMealPlan(username, mealPlan);
            myAgent.doDelete();
        }

        private List<Recipe> generateMealPlan(User user, List<Recipe> recipes) {
            List<Recipe> mealPlan = new ArrayList<>();
            double totalCalories = 0;
            double dailyCalorieGoal = user.getDailyCalorieGoal();
            int mealCount = numberOfMeals;

            while (totalCalories < dailyCalorieGoal && mealPlan.size() < mealCount) {
                Recipe selectedRecipe = selectRecipe(recipes, totalCalories, dailyCalorieGoal, mealPlan);
                if (selectedRecipe != null) {
                    mealPlan.add(selectedRecipe);
                    totalCalories += selectedRecipe.getCaloriesPer100gram();
                }
                System.out.println(selectedRecipe);
            }

            return mealPlan;
        }

        private Recipe selectRecipe(List<Recipe> recipes, double currentCalories, double dailyCalorieGoal, List<Recipe> mealPlan) {
            for (Recipe recipe : recipes) {
                System.out.println(recipe.getRecipeName());
                if (currentCalories + recipe.getCaloriesPer100gram() <= dailyCalorieGoal && !mealPlan.contains(recipe)) {
                    return recipe;
                }
            }
            return null;
        }
    }
}
