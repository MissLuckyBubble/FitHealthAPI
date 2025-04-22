package fit.health.fithealthapi.utils;

import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.repository.MealItemRepository;
import fit.health.fithealthapi.repository.MealRepository;
import jakarta.persistence.EntityManager;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

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

        if (!container.getAllergens().isEmpty()) {
            container.getAllergens().remove(Allergen.ALLERGEN_FREE);
        }

        // Add ALLERGEN_FREE if the set is still empty
        if (container.getAllergens().isEmpty()) {
            container.getAllergens().add(Allergen.ALLERGEN_FREE);
        }
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

    public static Meal getMealByType(MealContainer container, RecipeType type) {
        return switch (type) {
            case BREAKFAST -> container.getBreakfast();
            case LUNCH     -> container.getLunch();
            case DINNER    -> container.getDinner();
            case SNACK     -> container.getSnack();
            default        -> throw new IllegalArgumentException("Unsupported recipe type: " + type);
        };
    }

    public static void assignMealToSlot(MealContainer container, RecipeType type, Meal meal) {
        switch (type) {
            case BREAKFAST -> container.setBreakfast(meal);
            case LUNCH     -> container.setLunch(meal);
            case DINNER    -> container.setDinner(meal);
            case SNACK     -> container.setSnack(meal);
            default        -> throw new IllegalArgumentException("Unsupported recipe type: " + type);
        }
    }

    public static Meal ensureMealSlot(MealContainer container, RecipeType type, Supplier<Meal> createFunction) {
        Meal meal = getMealByType(container, type);
        if (meal == null) {
            meal = createFunction.get();
            assignMealToSlot(container, type, meal);
        }
        return meal;
    }

    public static void copyMealItemsSafeAppend(
            Meal original,
            Meal target,
            User user,
            MealItemRepository mealItemRepository,
            Float servingsFactor,
            EntityManager entityManager
    ) {
        float scale = (servingsFactor != null && servingsFactor > 0) ? servingsFactor : 1.0f;

        for (MealItem originalItem : original.getMealItems()) {
            MealItem newItem = new MealItem();
            newItem.setComponent(originalItem.getComponent());
            newItem.setQuantity(originalItem.getQuantity() * scale);
            newItem.setUnit(originalItem.getUnit());
            newItem.setOwner(user);
            newItem.setMeal(target);
            newItem.updateData();

            mealItemRepository.save(newItem);
            // No add to target.getMealItems(); we rely on @OneToMany reverse mapping
        }

        entityManager.flush();
        entityManager.refresh(target);
        target.updateMealData();
    }
}
