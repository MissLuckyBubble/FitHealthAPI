package fit.health.fithealthapi.agents;

import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class MealScoringAgent extends Agent {
    private RecipeService recipeService;
    private UserService userService;

    public void init(RecipeService recipeService, UserService userService) {
        this.recipeService = recipeService;
        this.userService = userService;
    }
    public MealScoringAgent() {
        // Default constructor for JADE
    }
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " (MealScoringAgent) started.");
        addBehaviour(new ScoreMealBehaviour());
    }
    private class ScoreMealBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST){
                System.out.println(getLocalName()+ " received a meal scoring request");
                String[] contentParts = msg.getContent().split(";");
                Long userId = Long.parseLong(contentParts[0]);
                Long recipeId = Long.parseLong(contentParts[1]);
                User user = userService.getUserById(userId);
                Recipe recipe = recipeService.getRecipeById(recipeId);
                
                if(user != null && recipe != null){
                    int score = calculateScore(user, recipe);
                    sendScoreToMealPlanAgent(msg.getSender().getLocalName(), recipeId, score);
                }else{
                    System.out.println("User with id " + userId + " or recipe with id " + recipeId + " not found, skipping scoring.");
                }
            }
        }

        private void sendScoreToMealPlanAgent(String localName, Long recipeId, int score) {
        }

        private int calculateScore(User user, Recipe recipe) {
            int score = 0;
            return 0;
        }

        private int calculateCalorieFitScore(float dailyCalorieGoal, float recipeCalories){
           return 0;
        }
    }
}
