package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

public class MealPlanAgent extends Agent {

    private UserService userService;
    private RecipeService recipeService;
    private User user;
    private List<Recipe> recipes;
    private int numberOfMeals;
    private int numberOfDays;

    public MealPlanAgent() {
        // Default constructor for JADE
    }

    public void init(UserService userService, RecipeService recipeService, User user, List<Recipe> recipes, int numberOfMeals, int numberOfDays) {
        this.userService = userService;
        this.recipeService = recipeService;
        this.user = user;
        this.recipes = recipes;
        this.numberOfMeals = numberOfMeals;
        this.numberOfDays = numberOfDays;
    }

    @Override
    protected void setup() {
        addBehaviour(new GenerateMealPlanBehaviour());
    }

    private class GenerateMealPlanBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                List<Recipe> mealPlan = generateMealPlan(user, recipes);
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(convertMealPlanToContent(mealPlan));
                myAgent.send(reply);
                myAgent.doDelete(); // Terminate the agent after sending the reply
            } else {
                block();
            }
        }

        private List<Recipe> generateMealPlan(User user, List<Recipe> recipes) {
            List<Recipe> mealPlan = new ArrayList<>();
            double totalCalories = 0;
            double dailyCalorieGoal = user.getDailyCalorieGoal();
            int totalMeals = numberOfMeals * numberOfDays;

            // First, collect all valid recipes that fit the criteria
            List<Recipe> validRecipes = new ArrayList<>();
            for (Recipe recipe : recipes) {
                if (recipe.getCaloriesPer100gram() <= dailyCalorieGoal) {
                    validRecipes.add(recipe);
                }
            }

            // Use the valid recipes to create the meal plan
            for (int i = 0; i < totalMeals; i++) {
                if (validRecipes.isEmpty()) {
                    break;
                }
                Recipe selectedRecipe = validRecipes.get(i % validRecipes.size());
                mealPlan.add(selectedRecipe);
                totalCalories += selectedRecipe.getCaloriesPer100gram();
            }


            return mealPlan;
        }

        private String convertMealPlanToContent(List<Recipe> mealPlan) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(mealPlan);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    protected void takeDown() {
        System.out.println("Meal Plan Agent " + getLocalName() + " terminated.");
    }

}
