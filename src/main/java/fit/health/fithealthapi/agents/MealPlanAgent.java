package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.RecipeSearchRequest;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.*;
import java.util.stream.Collectors;

public class MealPlanAgent extends Agent {

    private UserService userService;
    private RecipeService recipeService;

    public MealPlanAgent() {
        // Default constructor for JADE
    }

    public void init(UserService userService, RecipeService recipeService) {
        this.userService = userService;
        this.recipeService = recipeService;
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new GenerateMealPlanBehaviour());
    }

    private class GenerateMealPlanBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                String[] parts = msg.getContent().split(";");
                Long userId = Long.parseLong(parts[0]);
                Set<RecipeType> recipeTypes = extractMealTypes(parts[1]);
                User user = userService.getUserById(userId);
                List<Recipe> recommendedMeals = generateMealPlan(user, recipeTypes);
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(serializeMealPlan(recommendedMeals));
                myAgent.send(reply);
            } else {
                block();
            }
        }

        private Set<RecipeType> extractMealTypes(String mealTypeString) {
            Set<RecipeType> mealTypes = new HashSet<>();
            for (String type : mealTypeString.replace("[", "").replace("]", "").split(",")) {
                mealTypes.add(RecipeType.valueOf(type.trim().toUpperCase()));
            }
            return mealTypes;
        }

        private List<Recipe> generateMealPlan(User user, Set<RecipeType> recipeTypes) {
            float remainingCalories = user.getDailyCalorieGoal();
            List<Recipe> selectedMeals = new ArrayList<>();

            for (RecipeType type : recipeTypes) {
                RecipeSearchRequest searchRequest = new RecipeSearchRequest();
                searchRequest.setDietaryPreferences(user.getDietaryPreferences().stream().toList());
                searchRequest.setAllergens(user.getAllergens().stream().toList());
                searchRequest.setRecipeTypes(Collections.singletonList(type));
                searchRequest.setMaxCalories(remainingCalories); // Ensure calories fit
                searchRequest.setGoal(user.getGoal());
                List<Recipe> filteredRecipes = recipeService.searchRecipes(searchRequest);

                if (!filteredRecipes.isEmpty()) {
                    Recipe selected = filteredRecipes.get(0);
                    selectedMeals.add(selected);
                    remainingCalories -= selected.getCalories();
                }
            }
            return selectedMeals;
        }

        private String serializeMealPlan(List<Recipe> recipes) {
            return recipes.stream()
                    .map(recipe -> String.valueOf(recipe.getId())) // Convert ID to String
                    .collect(Collectors.joining(",")); // Join with commas
        }
    }

    protected void takeDown() {
        System.out.println("Meal Plan Agent " + getLocalName() + " terminated.");
    }

}
