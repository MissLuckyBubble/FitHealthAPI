package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class UserAgent extends Agent {

    private UserService userService;
    private RecipeService recipeService;
    private User user;
    private List<Recipe> recipes;
    private int numberOfMeals;
    private int numberOfDays;

    public UserAgent() {
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
        addBehaviour(new RequestMealPlanBehaviour());
        addBehaviour(new ReceiveMealPlanBehaviour());
    }

    private class RequestMealPlanBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            AID mealPlanAgent = new AID("mealPlanAgent" + user.getUsername(), AID.ISLOCALNAME);
            request.addReceiver(mealPlanAgent);
            request.setContent("Generate meal plan for user: " + user.getUsername());
            myAgent.send(request);
        }
    }

    private class ReceiveMealPlanBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                String mealPlanContent = msg.getContent();
                List<Recipe> mealPlan = convertContentToMealPlan(mealPlanContent);
                userService.saveMealPlan(user.getUsername(), mealPlan);
                myAgent.doDelete();
            } else {
                block();
            }
        }

        private List<Recipe> convertContentToMealPlan(String content) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return Arrays.asList(mapper.readValue(content, Recipe[].class));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("User Agent " + getLocalName() + " terminated.");
    }
}
