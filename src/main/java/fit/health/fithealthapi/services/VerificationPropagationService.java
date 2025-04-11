package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VerificationPropagationService {

    private final RecipeRepository recipeRepository;
    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final MealPlanRepository mealPlanRepository;

    private final MealPlanService mealPlanService;

    public void onFoodItemUpdated(FoodItem foodItem) {
        List<Recipe> recipes = recipeRepository.findAllByIngredient(foodItem);
        for (Recipe recipe : recipes) {
            recipe.checkAndUpdateVerification();
            recipeRepository.save(recipe);
            recipeRepository.flush();
            onRecipeUpdated(recipe);
        }

        updateComponent(foodItem);
    }

    public void onRecipeUpdated(Recipe recipe) {
        updateComponent(recipe);
    }

    private void updateComponent(MealComponent component) {
        List<MealItem> mealItems = mealItemRepository.findByComponent(component);
        for (MealItem item : mealItems) {
            item.updateData();
            mealItemRepository.save(item);
            mealItemRepository.flush();
            onMealItemUpdated(item);
        }
    }

    public void onMealItemUpdated(MealItem item) {
        Meal meal = item.getMeal();
        if (meal != null) {
            meal.updateMealData();
            mealRepository.save(meal);
            mealRepository.flush();
            onMealUpdated(meal);
        }
    }

    public void onMealUpdated(Meal meal) {
        List<MealPlan> plans = mealPlanRepository.findAllByMeal(meal);
        for (MealPlan plan : plans) {
            updateVerificationStatus(plan);
            mealPlanRepository.save(plan);
            mealPlanRepository.flush();
        }
    }

    private void updateVerificationStatus(MealPlan plan) {
        boolean allVerified = plan.getMeals().stream()
                .filter(Objects::nonNull)
                .allMatch(Meal::isVerifiedByAdmin);
        plan.setVerifiedByAdmin(allVerified);
    }
}
