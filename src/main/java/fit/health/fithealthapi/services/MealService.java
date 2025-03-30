package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.*;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.model.enums.Visibility;
import fit.health.fithealthapi.repository.FoodItemRepository;
import fit.health.fithealthapi.repository.MealItemRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MealService {
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final RecipeRepository recipeRepository;
    private final FoodItemRepository foodItemRepository;
    private final RecipeService recipeService;
    private final FoodItemService foodItemService;

    @Transactional
    public Meal createMeal(Meal meal, User user){
        meal.setUser(user);
        setMealItems(meal);
        try {
            updateMealData(meal);
            return mealRepository.save(meal);
        }catch (Exception e){
           System.out.println(e.getMessage());
        }
        return meal;
    }

    private void setMealItems(Meal meal) {
        if(meal.getMealItems() != null){
            for(MealItem item : meal.getMealItems()){
                item.setMeal(meal);
                if(item.getRecipe()!=null){
                    Recipe recipe = recipeService.getRecipeById(item.getRecipe().getId());
                    item.setRecipe(recipe);
                    item.setFoodItem(null);
                }else if(item.getFoodItem()!=null){
                    FoodItem foodItem = foodItemService.getById(item.getFoodItem().getId());
                    item.setFoodItem(foodItem);
                    item.setRecipe(null);
                }else{
                    throw new InvalidRequestException("Each meal item must have either a recipe or a food item");
                }
                item.prePersist();
            }
        }
    }

    public List<Meal> getUserMeals(User user) {
        return mealRepository.findByUser(user);
    }

    public List<Meal> getPublicMeals() {
        return mealRepository.findByVisibility(Visibility.PUBLIC);
    }

    @Transactional
    public Meal updateMeal(Meal meal, long id) {
        Meal existingMeal = mealRepository.findById(id).orElse(null);
        if(existingMeal == null){
            throw new NotFoundException("Meal not found");
        }
        existingMeal.setName(meal.getName());
        existingMeal.setRecipeType(meal.getRecipeType());
        existingMeal.setVisibility(meal.getVisibility());
        setMealItems(meal);
        try {
            updateMealData(meal);
            return mealRepository.save(meal);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return meal;
    }

    @Transactional
    public void deleteMeal(Long id) {
        mealRepository.deleteById(id);
    }

    public Optional<Meal> getMealById(Long id) {
        return mealRepository.findById(id);
    }

    public Meal processMealRequest(CreateMealRequestDto dto, User user) {
        Meal meal;

        if (dto.getMealId() != null) {
            // ✅ Modify an existing meal
            meal = mealRepository.findById(dto.getMealId())
                    .orElseThrow(() -> new NotFoundException("Meal not found"));
        } else {
            // ✅ Create a new meal
            meal = new Meal();
            meal.setName(dto.getMealName() != null ? dto.getMealName() : "");
            meal.setUser(user);
            meal.setRecipeType(dto.getRecipeType());
            Macronutrients macronutrients = new Macronutrients();
            meal.setMacronutrients(macronutrients);
            mealRepository.save(meal);
        }

        // ✅ Create a new MealItem (whether adding to existing meal or a new one)
        MealItem mealItem = new MealItem();
        mealItem.setMeal(meal);

        if (dto.getRecipeId() != null) {
            Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                    .orElseThrow(() -> new RecipeNotFoundException("Recipe not found"));
            mealItem.setRecipe(recipe);
            mealItem.setPortionSize(dto.getPortionSize());
        } else if (dto.getFoodItemId() != null) {
            FoodItem foodItem = foodItemRepository.findById(dto.getFoodItemId())
                    .orElseThrow(() -> new IngredientNotFoundException("FoodItem not found"));
            mealItem.setFoodItem(foodItem);
            mealItem.setWeightGrams(dto.getWeightGrams());
        } else {
            throw new InvalidRequestException("Either recipeId or foodItemId must be provided.");
        }
        mealItem.prePersist();
        mealItemRepository.save(mealItem);
        Set<MealItem> mealItems = meal.getMealItems() != null ? meal.getMealItems() : new HashSet<>();
        mealItems.add(mealItem);
        meal.setMealItems(mealItems);
        updateMealData(meal);
        mealRepository.save(meal);
        return meal;
    }

    public Meal removeMealItem(long mealItemId, User user){
        MealItem mealItem = mealItemRepository.findById(mealItemId).orElseThrow(()->new NotFoundException("Meal item not found"));
        if(!user.getRole().equals(Role.ADMIN) && !user.getId().equals(mealItem.getMeal().getUser().getId())){
            throw new ForbiddenException("You are not allowed to remove meal item");
        }
        Meal meal = mealItem.getMeal();
        mealItemRepository.deleteById(mealItemId);
        updateMealData(meal);
        return meal;
    }
    public List<Meal> searchMeals(MealSearchDto searchDto) {
        List<Meal> meals = mealRepository.findAll();

        if (searchDto.getQuery() != null && !searchDto.getQuery().isBlank()) {
            String searchText = searchDto.getQuery().toLowerCase();

            meals = meals.stream()
                    .filter(meal -> searchInMeal(meal, searchText))
                    .toList();
        }


        if (searchDto.getMealType() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getRecipeType() == searchDto.getMealType())
                    .toList();
        }

        if(searchDto.getUserId() != null){
            meals = meals.stream().filter(meal -> meal.getUser().getId().equals(searchDto.getUserId())).toList();
        }else {
            meals = meals.stream().filter(meal-> meal.getVisibility().equals(Visibility.PUBLIC)).toList();
        }

        if (searchDto.getDietaryPreferences() != null && !searchDto.getDietaryPreferences().isEmpty()) {
            meals = meals.stream()
                    .filter(meal -> meal.getDietaryPreferences().containsAll(searchDto.getDietaryPreferences()))
                    .toList();
        }

        if (searchDto.getHealthConditions() != null && !searchDto.getHealthConditions().isEmpty()) {
            meals = meals.stream()
                    .filter(meal -> meal.getHealthConditionSuitability().containsAll(searchDto.getHealthConditions()))
                    .toList();
        }

        if (searchDto.getExcludeAllergens() != null && !searchDto.getExcludeAllergens().isEmpty()) {
            meals = meals.stream()
                    .filter(meal -> meal.getAllergens().stream().noneMatch(searchDto.getExcludeAllergens()::contains))
                    .toList();
        }

        if (searchDto.getMinCalories() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getCalories() >= searchDto.getMinCalories())
                    .toList();
        }
        if (searchDto.getMaxCalories() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getCalories() <= searchDto.getMaxCalories())
                    .toList();
        }

        if (searchDto.getMinProtein() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getProtein() >= searchDto.getMinProtein())
                    .toList();
        }
        if (searchDto.getMaxProtein() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getProtein() <= searchDto.getMaxProtein())
                    .toList();
        }

        if (searchDto.getMinFat() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getFat() >= searchDto.getMinFat())
                    .toList();
        }
        if (searchDto.getMaxFat() != null) {
            meals = meals.stream()
                    .filter(meal -> meal.getMacronutrients().getFat() <= searchDto.getMaxFat())
                    .toList();
        }

        meals = meals.stream()
                .sorted(Comparator.comparing(Meal::isVerifiedByAdmin).reversed())
                .toList();

        if (searchDto.getSortBy() != null) {
            Comparator<Meal> comparator = null;

            switch (searchDto.getSortBy()) {
                case "likes" -> comparator = Comparator.comparingInt(meal -> meal.getUser().getFavoriteRecipes().size());
                case "date" -> comparator = Comparator.comparing(Meal::getId);
            }

            if (comparator != null) {
                if ("desc".equalsIgnoreCase(searchDto.getSortDirection())) {
                    comparator = comparator.reversed();
                }
                meals = meals.stream().sorted(comparator).toList();
            }
        }

        return meals;
    }

    public void updateMealData(Meal meal) {
        updateMacroNutrients(meal);
        updateMealSuitabilityAndAllergens(meal);
        updateDietaryPreferences(meal);
        updateVerificationStatus(meal);

        if ((meal.getName() == null || meal.getName().isBlank()) && meal.getUser() != null) {
            meal.setName(meal.getUser().getUsername() + "'s " + meal.getRecipeType().getDisplayName());
        }
    }

    private void updateMacroNutrients(Meal meal) {
        if (meal.getMacronutrients() == null) {
            meal.setMacronutrients(new Macronutrients());
        }
        meal.getMacronutrients().reset();

        for (MealItem item : meal.getMealItems()) {
            meal.getMacronutrients().add(item.getMacronutrients());
        }
    }

    private void updateMealSuitabilityAndAllergens(Meal meal) {
        if (meal.getMealItems() == null || meal.getMealItems().isEmpty()) {
            meal.getAllergens().clear();
            meal.getHealthConditionSuitability().clear();
            return;
        }

        meal.getAllergens().clear();
        for (MealItem item : meal.getMealItems()) {
            if (item.getAllergens() != null) {
                meal.getAllergens().addAll(item.getAllergens());
            }
        }

        Set<HealthConditionSuitability> commonConditions = new HashSet<>();
        boolean firstItem = true;
        for (MealItem item : meal.getMealItems()) {
            if (item.getHealthConditionSuitabilities() != null) {
                if (firstItem) {
                    commonConditions.addAll(item.getHealthConditionSuitabilities());
                    firstItem = false;
                } else {
                    commonConditions.retainAll(item.getHealthConditionSuitabilities());
                }
            }
        }
        meal.setHealthConditionSuitability(commonConditions);
    }

    private void updateVerificationStatus(Meal meal) {
        meal.setVerifiedByAdmin(meal.getMealItems().stream().allMatch(MealItem::isVerifiedByAdmin));
    }

    private void updateDietaryPreferences(Meal meal) {
        Set<DietaryPreference> commonPreferences = new HashSet<>();
        boolean firstItem = true;
        for (MealItem item : meal.getMealItems()) {
            if (item.getDietaryPreferences() != null) {
                if (firstItem) {
                    commonPreferences.addAll(item.getDietaryPreferences());
                    firstItem = false;
                } else {
                    commonPreferences.retainAll(item.getDietaryPreferences());
                }
            }
        }
        meal.setDietaryPreferences(commonPreferences);
    }

    public boolean searchInMeal(Meal meal, String searchText) {
        if (meal == null) return false;

        return meal.getName().toLowerCase().contains(searchText) ||
                meal.getMealItems().stream().anyMatch(item ->
                        item.getName() != null && item.getName().toLowerCase().contains(searchText)
                ) ||
                meal.getMealItems().stream().anyMatch(item ->
                        item.getFoodItem() != null &&
                                item.getFoodItem().getName() != null &&
                                item.getFoodItem().getName().toLowerCase().contains(searchText)
                ) ||
                meal.getMealItems().stream().anyMatch(item ->
                        item.getRecipe() != null &&
                                item.getRecipe().getName() != null &&
                                item.getRecipe().getName().toLowerCase().contains(searchText)
                ) ||
                meal.getMealItems().stream().anyMatch(item ->
                        item.getRecipe() != null &&
                                item.getRecipe().getDescription() != null &&
                                item.getRecipe().getDescription().toLowerCase().contains(searchText)
                ) ||
                meal.getMealItems().stream().anyMatch(item ->
                        item.getRecipe() != null &&
                                item.getRecipe().getIngredients() != null &&
                                item.getRecipe().getIngredients().stream().anyMatch(ingredient ->
                                        ingredient.getFoodItem() != null &&
                                                ingredient.getFoodItem().getName() != null &&
                                                ingredient.getFoodItem().getName().toLowerCase().contains(searchText)
                                )
                );
    }
}
