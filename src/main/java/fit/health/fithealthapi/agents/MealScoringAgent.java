package fit.health.fithealthapi.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.UserPreference;
import fit.health.fithealthapi.model.dto.agents.MealScoringRequest;
import fit.health.fithealthapi.model.dto.scoring.ScoringMealDto;
import fit.health.fithealthapi.model.dto.scoring.ScoringUserDto;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.Goal;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.UserItemType;
import fit.health.fithealthapi.services.UserPreferenceService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.*;
import java.util.stream.Collectors;

public class MealScoringAgent extends Agent {

    private transient UserPreferenceService userPreferenceService;

    public MealScoringAgent() {
        // Required for JADE
    }

    public void init(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " ready.");
        addBehaviour(new ScoreMealsBehaviour());
    }

    private class ScoreMealsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                System.out.println("Received meal scoring message");
                try {
                    MealScoringRequest request = objectMapper.readValue(msg.getContent(), MealScoringRequest.class);
                    Map<RecipeType, List<ScoringMealDto>> mealsByType = new HashMap<>();

                    for (ScoringMealDto meal : request.getMeals()) {
                        if (meal.getMacronutrients().getCalories() > 2000) continue;

                        List<RecipeType> mealTypes = meal.getRecipeTypes();
                        if (mealTypes == null || mealTypes.isEmpty()) continue;

                        for (RecipeType type : request.getRequestedTypes()) {
                            if (mealTypes.contains(type)) {
                                mealsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(meal);
                            }
                        }
                    }

                    Map<RecipeType, List<Long>> bestMeals = new HashMap<>();
                    int topN = Math.max(request.getDaysToGenerate(), 1);
                    int mealsPerDay = !request.getRequestedTypes().isEmpty() ? request.getRequestedTypes().size() : 3;

                    for (RecipeType type : request.getRequestedTypes()) {
                        List<ScoringMealDto> candidates = new ArrayList<>(mealsByType.getOrDefault(type, Collections.emptyList()));

                        System.out.println("Candidates for " + type + ": " + candidates.size());

                        candidates.sort((a, b) -> Float.compare(
                                scoreMeal(b, type, request, mealsPerDay),
                                scoreMeal(a, type, request, mealsPerDay)
                        ));

                        bestMeals.put(type, candidates.stream()
                                .limit(topN+3)
                                .map(ScoringMealDto::getId)
                                .collect(Collectors.toList()));
                    }

                    String result = objectMapper.writeValueAsString(bestMeals);
                    System.out.println("Score: " + result);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(result);
                    reply.setConversationId(msg.getConversationId());
                    myAgent.send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    private float scoreMeal(ScoringMealDto meal, RecipeType type, MealScoringRequest request, int mealsPerDay) {
        ScoringUserDto user = request.getUser();
        float score = 0;
        float dailyCal = getEffectiveCalorieGoal(user);

        score += 20 * calorieFitScore(meal, type, dailyCal, mealsPerDay);
        score += 15 * dietaryPreferencesScore(meal, user.getDietaryPreferences());
        score += 15 * macroBalanceScore(meal, user);
        score += 10 * preferenceHistoryScore(meal.getId(), user);
        score += 15 * typeMatchScore(meal, type);
        score += 5 * diversityPenalty(meal.getId(), user.getUsedMealIds());
        score += 5 * (meal.isVerifiedByAdmin() ? 1 : 0);
        score += 15 * (ingredientPreferenceScore(meal,user));
        return score;
    }

    private float getEffectiveCalorieGoal(ScoringUserDto user) {
        return user.getDailyCalorieGoal() > 0 ? user.getDailyCalorieGoal() : 2000f;
    }

    private float dietaryPreferencesScore(ScoringMealDto meal, Set<DietaryPreference> userPreferences) {
        if (userPreferences == null || userPreferences.isEmpty()) return 0.5f;

        Set<DietaryPreference> mealPrefs = meal.getDietaryPreferences();
        long matches = userPreferences.stream().filter(mealPrefs::contains).count();

        float matchRatio = (float) matches / userPreferences.size();

        boolean violatesStrict = userPreferences.contains(DietaryPreference.VEGAN) && !mealPrefs.contains(DietaryPreference.VEGAN)
                || userPreferences.contains(DietaryPreference.VEGETARIAN) && !mealPrefs.contains(DietaryPreference.VEGETARIAN)
                || userPreferences.contains(DietaryPreference.GLUTEN_FREE) && !mealPrefs.contains(DietaryPreference.GLUTEN_FREE)
                || userPreferences.contains(DietaryPreference.LACTOSE_FREE) && !mealPrefs.contains(DietaryPreference.LACTOSE_FREE);

        if (violatesStrict) return 0.0f;

        if (matchRatio == 1.0f) return 1f;
        if (matchRatio >= 0.5f) return 0.8f;
        return 0.5f;
    }


    private float calorieFitScore(ScoringMealDto meal, RecipeType type, float dailyTarget, int mealsPerDay) {
        float expectedMealCalories;
        if (mealsPerDay == 2) {
            expectedMealCalories = switch (type) {
                case LUNCH, DINNER -> dailyTarget * 0.5f;
                default -> dailyTarget / 2f;
            };
        } else if (mealsPerDay == 3) {
            expectedMealCalories = switch (type) {
                case BREAKFAST -> dailyTarget * 0.25f;
                case LUNCH -> dailyTarget * 0.40f;
                case DINNER -> dailyTarget * 0.35f;
                default -> dailyTarget / 3f;
            };
        } else {
            expectedMealCalories = dailyTarget / mealsPerDay;
        }

        float cal = meal.getMacronutrients().getCalories();
        float diff = Math.abs(cal - expectedMealCalories);
        if (diff <= expectedMealCalories * 0.1f) return 1f;
        if (diff <= expectedMealCalories * 0.2f) return 0.8f;
        return 0.5f;
    }

    private float macroBalanceScore(ScoringMealDto meal, ScoringUserDto user) {
        float protein = meal.getMacronutrients().getProtein();
        float calories = meal.getMacronutrients().getCalories();
        Goal goal = user.getGoal() != null ? user.getGoal() : Goal.MAINTAIN;
        float dailyCal = getEffectiveCalorieGoal(user);

        return switch (goal) {
            case FAT_LOSS_MODERATE -> {
                if (calories <= dailyCal * 0.85 && protein >= 20f) yield 1f;
                if (calories <= dailyCal * 0.95 && protein >= 15f) yield 0.8f;
                yield 0.5f;
            }
            case FAT_LOSS_AGGRESSIVE -> {
                if (calories <= dailyCal * 0.75 && protein >= 25f) yield 1f;
                if (calories <= dailyCal * 0.85 && protein >= 20f) yield 0.8f;
                yield 0.5f;
            }
            case MAINTAIN -> {
                if (protein >= 20f && calories >= dailyCal * 0.9 && calories <= dailyCal * 1.1) yield 1f;
                if (protein >= 15f && calories >= dailyCal * 0.8 && calories <= dailyCal * 1.2) yield 0.8f;
                yield 0.5f;
            }
            case GAIN_SLOW -> {
                if (calories >= dailyCal * 1.1 && protein >= 25f) yield 1f;
                if (calories >= dailyCal && protein >= 20f) yield 0.8f;
                yield 0.5f;
            }
            case GAIN_FAST -> {
                if (calories >= dailyCal * 1.2 && protein >= 30f) yield 1f;
                if (calories >= dailyCal * 1.1 && protein >= 25f) yield 0.8f;
                yield 0.5f;
            }
        };
    }


    private float preferenceHistoryScore(Long mealId, ScoringUserDto user) {
        boolean liked = userPreferenceService.getLikedItemsByType(user.getId(), UserItemType.MEAL)
                .stream().anyMatch(p -> p.getItemId().equals(mealId));
        if (liked) return 1f;

        boolean disliked = userPreferenceService.getDislikedItemsByType(user.getId(), UserItemType.MEAL)
                .stream().anyMatch(p -> p.getItemId().equals(mealId));
        return disliked ? 0f : 0.5f;
    }

    private float ingredientPreferenceScore(ScoringMealDto meal, ScoringUserDto user) {
        Set<Long> likedItemIds = userPreferenceService.getLikedItemsByType(user.getId(), UserItemType.FOOD_ITEM)
                .stream()
                .map(UserPreference::getItemId)
                .collect(Collectors.toSet());
        likedItemIds.addAll(userPreferenceService.getLikedItemsByType(user.getId(), UserItemType.RECIPE)
                .stream()
                .map(UserPreference::getItemId)
                .collect(Collectors.toSet()));

        Set<Long> dislikedItemIds = userPreferenceService.getDislikedItemsByType(user.getId(), UserItemType.FOOD_ITEM).stream()
                .map(UserPreference::getItemId)
                .collect(Collectors.toSet());
        dislikedItemIds.addAll(userPreferenceService.getDislikedItemsByType(user.getId(), UserItemType.RECIPE)
                .stream()
                .map(UserPreference::getItemId)
                .collect(Collectors.toSet()));

        long likedCount = meal.getComponentIds().stream().filter(likedItemIds::contains).count();
        long dislikedCount = meal.getComponentIds().stream().filter(dislikedItemIds::contains).count();
        int total = meal.getComponentIds().size();

        if (likedCount == total && dislikedCount == 0) return 1f;            // All liked
        if (likedCount > 0 && dislikedCount == 0) return 0.8f;               // Some liked, none disliked
        if (likedCount > 0) return 0.5f;                // Some liked, some disliked
        if (dislikedCount > 0) return 0.3f;               // None liked, some disliked
        return 0.5f;                                                         // Neutral
    }


    private float typeMatchScore(ScoringMealDto meal, RecipeType requestedType) {
        if (requestedType == null) return 0.5f;
        return meal.getRecipeTypes().contains(requestedType) ? 1f : 0.5f;
    }

    private float diversityPenalty(Long mealId, Set<Long> usedMeals) {
        if (usedMeals == null || usedMeals.isEmpty()) return 0.5f;
        return usedMeals.contains(mealId) ? -1f : 1f;
    }

    @Override
    protected void takeDown() {
        System.out.println("MealScoringAgent terminated.");
    }
}
