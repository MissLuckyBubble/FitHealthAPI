package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.ForbiddenException;
import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Visibility;
import fit.health.fithealthapi.repository.MealPlanRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.utils.MealContainerUtils;
import fit.health.fithealthapi.utils.MealSearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealRepository mealRepository;

    @Transactional
    public MealPlan createMealPlan(MealPlan mealPlan) {
        mealPlan.setBreakfast(loadMeal(mealPlan.getBreakfast()));
        mealPlan.setLunch(loadMeal(mealPlan.getLunch()));
        mealPlan.setDinner(loadMeal(mealPlan.getDinner()));
        mealPlan.setSnack(loadMeal(mealPlan.getSnack()));

        updateMealPlanData(mealPlan);

        return mealPlanRepository.save(mealPlan);
    }

    private Meal loadMeal(Meal partialMeal) {
        if (partialMeal == null) {
            return null;
        }
        Meal fullMeal = mealRepository.findByIdFullyLoaded(partialMeal.getId())
                .orElseThrow(() -> new NotFoundException("Meal not found"));

        if (fullMeal.getMacronutrients() != null) {
            // Force Hibernate to initialize all macronutrients fields
            System.out.println(fullMeal.getMacronutrients().getProtein());
            System.out.println(fullMeal.getMacronutrients().getSugar());
            System.out.println(fullMeal.getMacronutrients().getFat());
            System.out.println(fullMeal.getMacronutrients().getCalories());
        }

        return fullMeal;
    }


    public List<MealPlan> getOwnerMealPlans(User user) {
        return mealPlanRepository.findByOwner(user);
    }

    public void deleteMealPlan(Long id) {
        mealPlanRepository.deleteById(id);
    }

    public Optional<MealPlan> getMealPlanById(Long id) {
        return mealPlanRepository.findWithMealsById(id);
    }

    public void setMeal(CreateMealRequestDto assignMealDto) {
        MealPlan mealPlan = mealPlanRepository.findWithMealsById(assignMealDto.getDiaryEntryId()).orElseThrow(()->new MealPlanNotFoundException("Meal not found"));
        Meal meal = mealRepository.findById(assignMealDto.getMealId()).orElseThrow(()-> new NotFoundException("Meal not found"));

        switch (assignMealDto.getRecipeType()) {
            case BREAKFAST -> mealPlan.setBreakfast(meal);
            case LUNCH -> mealPlan.setLunch(meal);
            case DINNER -> mealPlan.setDinner(meal);
            case SNACK -> mealPlan.setSnack(meal);
        }
        mealPlanRepository.save(mealPlan);
    }

    public void updateMealPlanData(MealPlan mealPlan) {
        MealContainerUtils.updateMealContainerData(mealPlan);
    }


    public MealPlan removeMeal(Long mealPlanId, RecipeType recipeType, User user) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId).orElseThrow(()->new MealPlanNotFoundException("Meal plan not found."));
        if(!mealPlan.getOwner().getId().equals(user.getId())){
            throw new ForbiddenException("You are not allowed to remove this meal.");
        }

        MealContainerUtils.removeMealByType(mealPlan, recipeType);
        updateMealPlanData(mealPlan);
        return mealPlanRepository.save(mealPlan);
    }

    public List<MealPlan> searchMealPlan(MealSearchDto dto) {
        List<MealPlan> all = mealPlanRepository.findAll();
        return MealSearchUtils.filterMeals(all, dto);
    }

    public MealPlan createFromMealIds(User user, String name, Map<RecipeType, Long> idsByType) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setName(name);
        mealPlan.setOwner(user);
        mealPlan.setVerifiedByAdmin(false);
        mealPlan.setVisibility(Visibility.PUBLIC);
        mealPlan.setMacronutrients(new Macronutrients());

        // Assign meals
        for (Map.Entry<RecipeType, Long> entry : idsByType.entrySet()) {
            RecipeType type = entry.getKey();
            Long mealId = entry.getValue();

            Meal meal = mealRepository.findById(mealId).orElseThrow(()->new NotFoundException("Meal not found"));

            switch (type) {
                case BREAKFAST -> mealPlan.setBreakfast(meal);
                case LUNCH -> mealPlan.setLunch(meal);
                case DINNER -> mealPlan.setDinner(meal);
                case SNACK -> mealPlan.setSnack(meal);
            }
        }

        updateMealPlanData(mealPlan);
        return mealPlanRepository.save(mealPlan);
    }

    public List<MealPlan> getAllByUser(User user) {
        return mealPlanRepository.findByOwner(user);
    }
}
