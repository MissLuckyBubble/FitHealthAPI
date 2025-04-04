package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.*;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.model.enums.Visibility;
import fit.health.fithealthapi.repository.FoodItemRepository;
import fit.health.fithealthapi.repository.MealItemRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.repository.RecipeRepository;
import fit.health.fithealthapi.utils.MealSearchUtils;
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
        meal.setOwner(user);
        setMealItems(meal);
        try {
            meal.updateMealData();
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
                if (item.getComponent() == null) {
                    throw new InvalidRequestException("Each meal item must have a component (recipe or food item)");
                }

                MealComponent component = item.getComponent();
                if (component instanceof Recipe recipe) {
                    recipe = recipeService.getRecipeById(recipe.getId());
                    item.setPortionSize(item.getPortionSize());
                    item.setComponent(recipe);
                } else if (component instanceof FoodItem foodItem) {
                    foodItem = foodItemService.getById(foodItem.getId());
                    item.setWeightGrams(item.getWeightGrams());
                    item.setComponent(foodItem);
                } else {
                    throw new InvalidRequestException("Unsupported component type.");
                }

                item.updateData();
            }
        }
    }

    public List<Meal> getUserMeals(User user) {
        return mealRepository.findByOwner(user);
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
        existingMeal.setRecipeTypes(meal.getRecipeTypes());
        existingMeal.setVisibility(meal.getVisibility());
        setMealItems(meal);
        try {
            meal.updateMealData();
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
            meal = mealRepository.findById(dto.getMealId())
                    .orElseThrow(() -> new NotFoundException("Meal not found"));
        } else {
            meal = new Meal();
            meal.setName(dto.getMealName() != null ? dto.getMealName() : "");
            meal.setOwner(user);
            Set<RecipeType> recipeTypeSet = meal.getRecipeTypes();
            recipeTypeSet.add(dto.getRecipeType() != null ? dto.getRecipeType() : null);
            meal.setRecipeTypes(recipeTypeSet);
            Macronutrients macronutrients = new Macronutrients();
            meal.setMacronutrients(macronutrients);
            mealRepository.save(meal);
        }

        MealItem mealItem = new MealItem();
        mealItem.setMeal(meal);

        if (dto.getRecipeId() != null) {
            Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                    .orElseThrow(() -> new RecipeNotFoundException("Recipe not found"));
            mealItem.setComponent(recipe);
            mealItem.setPortionSize(dto.getPortionSize());
        } else if (dto.getFoodItemId() != null) {
            FoodItem foodItem = foodItemRepository.findById(dto.getFoodItemId())
                    .orElseThrow(() -> new IngredientNotFoundException("FoodItem not found"));
            mealItem.setComponent(foodItem);
            mealItem.setWeightGrams(dto.getWeightGrams());
        } else {
            throw new InvalidRequestException("Either recipeId or foodItemId must be provided.");
        }

        mealItem.updateData();
        mealItemRepository.save(mealItem);
        Set<MealItem> mealItems = meal.getMealItems() != null ? meal.getMealItems() : new HashSet<>();
        mealItems.add(mealItem);
        meal.setMealItems(mealItems);
        meal.updateMealData();
        mealRepository.save(meal);
        return meal;
    }

    public Meal removeMealItem(long mealItemId, User user){
        MealItem mealItem = mealItemRepository.findById(mealItemId).orElseThrow(()->new NotFoundException("Meal item not found"));
        if(!user.getRole().equals(Role.ADMIN) && !user.getId().equals(mealItem.getMeal().getOwner().getId())){
            throw new ForbiddenException("You are not allowed to remove meal item");
        }
        Meal meal = mealItem.getMeal();
        mealItemRepository.deleteById(mealItemId);
        meal.updateMealData();
        return meal;
    }

    public List<Meal> searchMeals(MealSearchDto dto) {
        List<Meal> all = mealRepository.findAll();
        return MealSearchUtils.filterMeals(all, dto);
    }

}
