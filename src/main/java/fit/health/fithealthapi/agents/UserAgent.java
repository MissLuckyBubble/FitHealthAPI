package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.MealPlanService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class UserAgent extends Agent {

    private transient MealPlanService mealPlanService;
    private transient User user;
    private Set<RecipeType> recipeTypes;
    private int days;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserAgent(){
        //do not delete it's needed for JADE
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
        addBehaviour(new RequestMealPlansBehaviour());
        addBehaviour(new ReceiveMealPlansBehaviour());
    }

    private class RequestMealPlansBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            try {
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", user.getId());
                requestMap.put("recipeTypes", recipeTypes.stream().map(Enum::name).collect(Collectors.toList()));
                requestMap.put("days", days);

                String jsonRequest = objectMapper.writeValueAsString(requestMap);

                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(getAID("MealPlanAgent"));
                request.setContent(jsonRequest);
                myAgent.send(request);
                System.out.println("UserAgent (" + getLocalName() + ") sent meal plan generation request.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveMealPlansBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                try {
                    String content = msg.getContent();

                    List<Map<String, Long>> dailyMealTypeIds = objectMapper.readValue(
                            content,
                            new TypeReference<>() {}
                    );

                    int dayIndex = 1;
                    for (Map<String, Long> mealIds : dailyMealTypeIds) {
                        Map<RecipeType, Long> idsByType = new HashMap<>();
                        for (Map.Entry<String, Long> entry : mealIds.entrySet()) {
                            idsByType.put(RecipeType.valueOf(entry.getKey()), entry.getValue());
                        }

                        String name = String.format("%s_AIPlan_%s_Day%d",
                                user.getUsername(),
                                new SimpleDateFormat("yyyyMMdd").format(new Date()),
                                dayIndex++
                        );

                        MealPlan plan = mealPlanService.createFromMealIds(user, name, idsByType);
                        System.out.println("Saved meal plan: " + plan.getName());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    myAgent.doDelete();
                }
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("UserAgent " + getLocalName() + " terminated.");
    }
}
