package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.RecipeNotFoundException;
import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.repository.MealPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MealPlanService {

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;

    /**
     * Create a new MealPlan.
     */
    @Transactional
    public MealPlan createMealPlan(String name, User user, Set<Long> recipeIds) {

        MealPlan mealPlan = new MealPlan();
        mealPlan.setName(name);
        mealPlan.setUser(user);

        Set<Recipe> recipes = recipeService.getRecipesByIds(recipeIds);
        mealPlan.setRecipes(recipes);
        mealPlan.setTotalCalories(calculateTotalCalories(recipes));

        return mealPlanRepository.save(mealPlan);
    }

    /**
     * Update an existing MealPlan.
     */
    @Transactional
    public MealPlan updateMealPlan(MealPlan mealPlan, String newName, Set<Long> recipeIds) {
        if (newName != null) {
            mealPlan.setName(newName);
        }

        if (recipeIds != null) {
            mealPlan.getRecipes().clear();

            Set<Recipe> recipes = recipeIds.stream()
                    .map(recipeService::getRecipeById)
                    .collect(Collectors.toSet());
            mealPlan.setRecipes(recipes);

            float totalCalories = calculateTotalCalories(recipes);
            mealPlan.setTotalCalories(totalCalories);

            return mealPlanRepository.save(mealPlan);
        }

        float totalCalories = calculateTotalCalories(mealPlan.getRecipes());
        mealPlan.setTotalCalories(totalCalories);

        return mealPlanRepository.save(mealPlan);
    }


    /**
     * Delete a MealPlan.
     */
    @Transactional
    public void deleteMealPlan(Long mealPlanId) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new MealPlanNotFoundException("Meal Plan not found."));
        mealPlanRepository.delete(mealPlan);
    }

    /**
     * Get a MealPlan by ID.
     */
    @Transactional(readOnly = true)
    public MealPlan getMealPlanById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new MealPlanNotFoundException("Meal Plan not found."));
    }

    /**
     * Get all MealPlans for a User.
     */
    @Transactional(readOnly = true)
    public Set<MealPlan> getMealPlansForUser(Long userId) {
        try{
            User user = userService.getUserById(userId);
            return mealPlanRepository.findByUser(user);
        }catch (UserNotFoundException e){
            throw new UserNotFoundException("User not found.");
        }
    }

    /**
     * Calculate total calories for a set of recipes.
     */
    private Float calculateTotalCalories(Set<Recipe> recipes) {
        return recipes.stream()
                .map(Recipe::getCalories)
                .reduce(0f, Float::sum);
    }

    public List<MealPlan> findMealPlans(Map<String, Object> filterMap, String sortField, String sortOrder) {

        List<MealPlan> all = mealPlanRepository.findAll();

        if (filterMap.containsKey("name")) {
            String nameFilter = filterMap.get("name").toString().toLowerCase();
            all = all.stream()
                    .filter(mp -> mp.getName() != null && mp.getName().toLowerCase().contains(nameFilter))
                    .collect(Collectors.toList());
        }

        Comparator<MealPlan> comparator;
        switch (sortField) {
            case "name":
                comparator = Comparator.comparing(MealPlan::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "id":
            default:
                comparator = Comparator.comparing(MealPlan::getId);
                break;
        }
        if ("DESC".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        all.sort(comparator);

        return all;
    }

}
