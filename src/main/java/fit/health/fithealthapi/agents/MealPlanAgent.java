package fit.health.fithealthapi.agents;

import fit.health.fithealthapi.model.MealComponent;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.MealComponentSearchRequest;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.MealComponentSearchService;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

public class MealPlanAgent extends Agent {

    private UserService userService;
    private MealComponentSearchService mealComponentSearchService;

    public MealPlanAgent() {
        // Default constructor for JADE do not delete
    }

    public void init(UserService userService, MealComponentSearchService mealComponentSearchService) {
        this.userService = userService;
        this.mealComponentSearchService = mealComponentSearchService;
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
            if (msg != null) {
               switch (msg.getPerformative()) {
                   case ACLMessage.REQUEST:
                       generateMealPlan(msg);
                       break;
                   case ACLMessage.INFORM:

               }
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

        private void generateMealPlan(ACLMessage msg) {
            String[] content = msg.getContent().split(";");
            Long userId = Long.parseLong(content[0]);
            int days = Integer.parseInt(content[2]);

            System.out.println(getLocalName() + " received request for " + days + " days of meal plans.");

            User user = userService.getUserById(userId);

            MealComponentSearchRequest searchRequest = new MealComponentSearchRequest();
            searchRequest.setDietaryPreferences(user.getDietaryPreferences().stream().toList());
            searchRequest.setAllergens(user.getAllergens().stream().toList());
            searchRequest.setMaxCalories(user.getDailyCalorieGoal());
            searchRequest.setGoal(user.getGoal());
            searchRequest.setOnlyRecipes(true);

            List<Long> filteredRecipesIds = mealComponentSearchService.searchMealComponents(searchRequest).stream().map(MealComponent::getId).toList();
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
