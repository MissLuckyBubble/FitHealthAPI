package fit.health.fithealthapi.utils;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealContainer;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.RecipeType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MealContainerUtils {

    public static void updateMealContainerData(MealContainer container) {
        updateVerification(container);
        updateAllergens(container);
        updateHealthConditions(container);
        updateDietaryPreferences(container);
        updateMacronutrients(container);
    }

    private static void updateVerification(MealContainer container) {
        boolean allVerified = container.getMeals().stream()
                .allMatch(meal -> meal == null || meal.isVerifiedByAdmin());
        container.setVerifiedByAdmin(allVerified);
    }

    private static void updateAllergens(MealContainer container) {
        container.getAllergens().clear();
        container.getMeals().stream()
                .filter(Objects::nonNull)
                .forEach(meal -> container.getAllergens().addAll(meal.getAllergens()));
    }

    private static void updateHealthConditions(MealContainer container) {
        Set<HealthConditionSuitability> common = new HashSet<>();
        boolean first = true;
        for (Meal meal : container.getMeals()) {
            if (meal == null) continue;
            Set<HealthConditionSuitability> suitability = meal.getHealthConditionSuitabilities();
            if (first) {
                common.addAll(suitability);
                first = false;
            } else {
                common.retainAll(suitability);
            }
        }
        container.setHealthConditionSuitabilities(common);
    }

    private static void updateDietaryPreferences(MealContainer container) {
        Set<DietaryPreference> common = new HashSet<>();
        boolean first = true;
        for (Meal meal : container.getMeals()) {
            if (meal == null) continue;
            Set<DietaryPreference> preferences = meal.getDietaryPreferences();
            if (first) {
                common.addAll(preferences);
                first = false;
            } else {
                common.retainAll(preferences);
            }
        }
        container.setDietaryPreferences(common);
    }

    private static void updateMacronutrients(MealContainer container) {
        Macronutrients macronutrients = new Macronutrients();

        for (Meal meal : container.getMeals()) {
            if (meal != null && meal.getMacronutrients() != null) {
                macronutrients.add(meal.getMacronutrients());
            }
        }
        container.setMacronutrients(macronutrients);
    }

    public static void removeMealByType(MealContainer container, RecipeType type) {
        switch (type) {
            case BREAKFAST -> container.setBreakfast(null);
            case LUNCH     -> container.setLunch(null);
            case DINNER    -> container.setDinner(null);
            case SNACK     -> container.setSnack(null);
            default        -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}
