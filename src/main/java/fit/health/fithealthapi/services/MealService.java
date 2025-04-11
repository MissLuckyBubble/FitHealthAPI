package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.*;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealDto;
import fit.health.fithealthapi.model.dto.MealItemDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealService {
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final RecipeRepository recipeRepository;
    private final FoodItemRepository foodItemRepository;
    private final VerificationPropagationService verificationPropagationService;

    @Transactional
    public Meal createMealFromDto(MealDto dto, User user) {
        Meal meal = new Meal();
        meal.setName(dto.getName());
        meal.setOwner(user);
        meal.setRecipeTypes(dto.getRecipeTypes());
        meal.setMacronutrients(new Macronutrients());

        Set<MealItem> mealItems = dto.getMealItems().stream()
                .map(itemDto -> buildMealItemFromDto(itemDto, meal, user))
                .collect(Collectors.toSet());

        meal.setMealItems(mealItems);
        meal.updateMealData();

        return mealRepository.save(meal);
    }


    private MealItem buildMealItemFromDto(MealItemDto itemDto, Meal meal, User user) {
        MealItem item = new MealItem();
        item.setMeal(meal);
        item.setOwner(user);
        item.setQuantity(itemDto.getQuantity());
        item.setUnit(itemDto.getUnit());

        MealComponent component = recipeRepository.findById(itemDto.getComponentId())
                .map(r -> (MealComponent) r)
                .orElseGet(() -> foodItemRepository.findById(itemDto.getComponentId())
                        .orElseThrow(() -> new NotFoundException("Component not found: " + itemDto.getComponentId())));

        item.setComponent(component);
        item.updateData();
        return item;
    }


    public List<Meal> getUserMeals(User user) {
        return mealRepository.findByOwner(user);
    }

    public List<Meal> getPublicMeals() {
        return mealRepository.findByVisibility(Visibility.PUBLIC);
    }

    @Transactional
    public Meal updateMealFromDto(MealDto dto, long id, User user) {
        Meal existingMeal = mealRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Meal not found"));

        existingMeal.setName(dto.getName());
        existingMeal.setRecipeTypes(dto.getRecipeTypes());
        existingMeal.setOwner(user);
        existingMeal.setMacronutrients(new Macronutrients());

        Set<MealItem> updatedItems = dto.getMealItems().stream()
                .map(itemDto -> buildMealItemFromDto(itemDto, existingMeal, user))
                .collect(Collectors.toSet());

        existingMeal.getMealItems().clear();
        existingMeal.getMealItems().addAll(updatedItems);
        existingMeal.updateMealData();
        verificationPropagationService.onMealUpdated(existingMeal);
        return mealRepository.save(existingMeal);
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

        // 1. Create or fetch existing Meal
        if (dto.getMealId() != null) {
            meal = mealRepository.findById(dto.getMealId())
                    .orElseThrow(() -> new NotFoundException("Meal not found"));
        } else {
            meal = new Meal();
            meal.setName(dto.getMealName() != null ? dto.getMealName() : "");
            meal.setOwner(user);
            if (dto.getRecipeType() != null) {
                meal.getRecipeTypes().add(dto.getRecipeType());
            }
            meal.setMacronutrients(new Macronutrients());
            mealRepository.save(meal);
        }

        // 2. Create MealItem
        MealItem mealItem = new MealItem();
        mealItem.setMeal(meal);

        if (dto.getComponentId() == null || dto.getComponentType() == null) {
            throw new InvalidRequestException("componentId and componentType are required.");
        }

        // 3. Resolve Component
        MealComponent component;
        switch (dto.getComponentType().toUpperCase()) {
            case "RECIPE" -> {
                component = recipeRepository.findById(dto.getComponentId())
                        .orElseThrow(() -> new RecipeNotFoundException("Recipe not found"));
            }
            case "FOOD_ITEM" -> {
                component = foodItemRepository.findById(dto.getComponentId())
                        .orElseThrow(() -> new IngredientNotFoundException("FoodItem not found"));
            }
            default -> throw new InvalidRequestException("Invalid componentType: must be RECIPE or FOOD_ITEM");
        }

        mealItem.setComponent(component);

        // 4. Set quantity and unit
        if (dto.getQuantity() == null || dto.getUnit() == null) {
            throw new InvalidRequestException("Both quantity and unit are required.");
        }
        mealItem.setQuantity(dto.getQuantity());
        mealItem.setUnit(dto.getUnit());

        // 5. Finalize and save
        mealItem.updateData();
        mealItemRepository.save(mealItem);

        meal.getMealItems().add(mealItem);
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
