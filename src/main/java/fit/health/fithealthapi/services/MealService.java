package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.*;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealDto;
import fit.health.fithealthapi.model.dto.MealItemDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.dto.scoring.ScoringMealDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.model.enums.Visibility;
import fit.health.fithealthapi.repository.*;
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
    private final MealComponentRepository mealComponentRepository;

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
        meal.setVisibility(dto.getVisibility());
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

    @Transactional
    public void addMealItemToMeal(CreateMealRequestDto dto, Meal meal, User user) {
        MealItem mealItem = new MealItem();
        mealItem.setMeal(meal);
        mealItem.setOwner(user);

        if(dto.getComponentId()!=null) {
            MealComponent component = mealComponentRepository.findByIdWithItems(dto.getComponentId()).orElseThrow(() -> new NotFoundException("Component not found"));
        mealItem.setComponent(component);
        mealItem.setQuantity(dto.getQuantity());
        mealItem.setUnit(dto.getUnit());}

        mealItem.updateData();
        mealItemRepository.save(mealItem);

        meal.getMealItems().add(mealItem);
        meal.updateMealData();
        mealRepository.save(meal);
    }


    public Meal removeMealItem(long mealItemId, User user){
        MealItem mealItem = mealItemRepository.findById(mealItemId).orElseThrow(()->new NotFoundException("Meal item not found"));
        if(!user.getRole().equals(Role.ADMIN) && !user.getId().equals(mealItem.getMeal().getOwner().getId())){
            throw new ForbiddenException("You are not allowed to remove meal item");
        }
        Meal meal = mealItem.getMeal();
        meal.getMealItems().remove(mealItem);
        mealItemRepository.deleteById(mealItemId);
        meal.updateMealData();
        mealRepository.save(meal);
        return meal;
    }

    public List<Meal> searchMeals(MealSearchDto dto) {
        List<Meal> all = mealRepository.findAll();
        return MealSearchUtils.filterMeals(all, dto);
    }

    public List<ScoringMealDto> searchScoringMealDto(MealSearchDto dto) {
        return toScoringDto(searchMeals(dto));
    }

    public List<ScoringMealDto> toScoringDto(List<Meal> meals) {
        return meals.stream().map(meal -> {
            ScoringMealDto dto = new ScoringMealDto();
            dto.setId(meal.getId());
            dto.setVerifiedByAdmin(meal.isVerifiedByAdmin());
            dto.setMacronutrients(meal.getMacronutrients());
            dto.setDietaryPreferences(meal.getDietaryPreferences());

            List<RecipeType> types = meal.getRecipeTypes().stream().toList();
            dto.setRecipeTypes(types);

            List<Long> componentIds = meal.getMealItems().stream()
                    .map(mealItem -> mealItem.getComponent().getId()).toList();
            dto.setComponentIds(componentIds);
            return dto;
        }).toList();
    }

}
