package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.dto.agents.MealPlanRequestContext;
import fit.health.fithealthapi.model.dto.agents.MealScoringRequest;
import fit.health.fithealthapi.model.dto.scoring.ScoringMealDto;
import fit.health.fithealthapi.model.dto.scoring.ScoringUserDto;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.MealService;
import fit.health.fithealthapi.services.SharedService;
import fit.health.fithealthapi.services.UserService;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.*;
import java.util.stream.Collectors;

public class MealPlanAgent extends Agent {

    private transient UserService userService;
    private transient MealService mealService;
    private transient SharedService sharedService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MealPlanAgent() {
        // DO NOT DELETE IT'S NEEDED FOR JADE!!!!
    }

    private final Map<Long, MealPlanRequestContext> activeRequests = new HashMap<>();

    public void init(UserService userService, MealService mealService, SharedService sharedService) {
        this.userService = userService;
        this.mealService = mealService;
        this.sharedService = sharedService;
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new HandleUserMealRequestBehaviour());
        addBehaviour(new ReceiveScoredMealsBehaviour());
    }

    private class HandleUserMealRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                System.out.println(getLocalName() + " received meal request.");
                try {
                    Map<String, Object> request = objectMapper.readValue(msg.getContent(), new TypeReference<>() {});
                    Long userId = Long.parseLong(request.get("userId").toString());
                    int days = Integer.parseInt(request.get("days").toString());
                    List<String> typeStrings = (List<String>) request.get("recipeTypes");

                    Set<RecipeType> recipeTypes = typeStrings.stream()
                            .map(RecipeType::valueOf)
                            .collect(Collectors.toSet());

                    AID senderAID = msg.getSender();
                    ScoringUserDto user = userService.getScoringUserById(userId);

                    MealSearchDto dto = new MealSearchDto();
                    dto.setAllergens(user.getAllergens());
                    Set<HealthConditionSuitability> healthConditionSuitabilities =
                            new HashSet<>(sharedService.convertToHealthConditionSuitability(
                                    user.getHealthConditions().stream().map(Enum::toString).toList()));
                    dto.setHealthConditions(healthConditionSuitabilities);
                    dto.setMinCalories(1f);

                    List<ScoringMealDto> meals = mealService.searchScoringMealDto(dto);

                    MealPlanRequestContext context = new MealPlanRequestContext(
                            senderAID,
                            user,
                            days,
                            recipeTypes,
                            meals
                    );

                    activeRequests.put(userId, context);

                    MealScoringRequest scoringRequest = new MealScoringRequest();
                    scoringRequest.setUser(user);
                    scoringRequest.setMeals(meals);
                    scoringRequest.setRequestedTypes(new ArrayList<>(recipeTypes));

                    System.out.println("Sending scoring request: " + scoringRequest);
                    ACLMessage scoreRequest = new ACLMessage(ACLMessage.REQUEST);
                    scoreRequest.addReceiver(getAID("MealScoringAgent"));
                    scoreRequest.setContent(objectMapper.writeValueAsString(scoringRequest));
                    scoreRequest.setConversationId("meal-plan-" + userId);
                    myAgent.send(scoreRequest);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    private class ReceiveScoredMealsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                System.out.println(getLocalName() + " received meal response.");
                try {
                    Map<String, List<Long>> bestMealsMap = objectMapper.readValue(msg.getContent(), new TypeReference<>() {});
                    String convId = msg.getConversationId();
                    if (convId == null || !convId.startsWith("meal-plan-")) {
                        System.err.println("Missing or invalid conversationId in scoring response");
                        return;
                    }

                    Long userId = Long.parseLong(convId.replace("meal-plan-", ""));
                    MealPlanRequestContext context = activeRequests.remove(userId);
                    if (context == null) return;

                    List<Map<String, Long>> planPerDay = new ArrayList<>();
                    Map<RecipeType, Queue<Long>> rotatingMeals = new HashMap<>();

                    // Setup rotating queues for each recipe type
                    bestMealsMap.forEach((typeStr, mealIds) -> {
                        RecipeType type = RecipeType.valueOf(typeStr.toUpperCase());
                        rotatingMeals.put(type, new LinkedList<>(mealIds));
                    });

                    // Build daily plans by rotating through the top meals
                    for (int i = 0; i < context.getDays(); i++) {
                        Map<String, Long> dayPlan = new HashMap<>();
                        for (RecipeType type : context.getRequestedTypes()) {
                            Queue<Long> queue = rotatingMeals.getOrDefault(type, new LinkedList<>());
                            if (!queue.isEmpty()) {
                                Long mealId = queue.poll();
                                dayPlan.put(type.name(), mealId);
                                queue.offer(mealId); // rotate for future days
                            }
                        }
                        planPerDay.add(dayPlan);
                    }

                    ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                    reply.addReceiver(context.getSenderAID());
                    reply.setContent(objectMapper.writeValueAsString(planPerDay));
                    myAgent.send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }


    @Override
    protected void takeDown() {
        System.out.println("MealPlanAgent terminated.");
    }
}
