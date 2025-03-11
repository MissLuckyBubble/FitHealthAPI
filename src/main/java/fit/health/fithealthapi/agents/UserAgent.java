package fit.health.fithealthapi.agents;


import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.MealPlanService;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class UserAgent extends Agent {

    private MealPlanService mealPlanService;
    private User user;
    Set<RecipeType> recipeTypes;
    private int days;

    public UserAgent() {
        // Default constructor for JADE
    }

    public void init(MealPlanService mealPlanService, User user, Set<RecipeType> recipeTypes, int days) {
        this.mealPlanService = mealPlanService;
        this.user = user;
        this.recipeTypes = recipeTypes;
        this.days = days;
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new RequestMealPlanBehaviour());
        addBehaviour(new ReceiveMealPlanBehaviour(days));
    }

    private class RequestMealPlanBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(getAID("MealPlanAgent"));
            request.setContent(user.getId()+";"+recipeTypes.toString()+";"+days);
            myAgent.send(request);
            System.out.println("UserAgent(" +getLocalName()+"): Sent request to Meal Recommendation Agent.");
        }
    }

    private class ReceiveMealPlanBehaviour extends CyclicBehaviour {
        private int remainingDays;
        public ReceiveMealPlanBehaviour(int days) {
            this.remainingDays = days;
        }
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                String mealPlanContent = msg.getContent();
                System.out.println("User Agent received meal plan: " + mealPlanContent);

                Set<Long> mealPlan = convertContentToMealPlan(mealPlanContent);
                if(mealPlan != null){
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
                    String formattedDate = dateFormat.format(new Date());
                    String mealPlanName = user.getUsername() + "_AIGenerated_" + formattedDate + "_" + (remainingDays) + "_days";
                    //mealPlanService.createMealPlan(mealPlanName, user, mealPlan);
                    remainingDays--;
                }

                if(remainingDays<=0){
                    myAgent.doDelete();
                }

            } else {
                block();
            }
        }

        public Set<Long> convertContentToMealPlan(String content) {
             Set<Long> recipeIds = Arrays.stream(content.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
             return  recipeIds;
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("User Agent " + getLocalName() + " terminated.");
    }
}
