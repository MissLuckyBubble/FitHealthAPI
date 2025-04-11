package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.ForbiddenException;
import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.repository.MealPlanRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.utils.MealContainerUtils;
import fit.health.fithealthapi.utils.MealSearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealRepository mealRepository;

    public MealPlan createMealPlan(MealPlan mealPlan) {
        if(mealPlan.getBreakfast() != null){
            mealPlan.setBreakfast(mealRepository.findById(mealPlan.getBreakfast().getId()).orElseThrow(()-> new NotFoundException("Breakfast not found")));
        }
        if(mealPlan.getLunch() != null){
            mealPlan.setLunch(mealRepository.findById(mealPlan.getLunch().getId()).orElseThrow(()-> new NotFoundException("Lunch not found")));
        }
        if(mealPlan.getDinner() != null){
            mealPlan.setDinner(mealRepository.findById(mealPlan.getDinner().getId()).orElseThrow(()-> new NotFoundException("Dinner not found")));
        }
        if(mealPlan.getSnack() != null){
            mealPlan.setSnack(mealRepository.findById(mealPlan.getSnack().getId()).orElseThrow(()-> new NotFoundException("Snack not found")));
        }
        try{
            updateMealPlanData(mealPlan);
            return mealPlanRepository.save(mealPlan);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return mealPlan;
    }

    public List<MealPlan> getOwnerMealPlans(User user) {
        return mealPlanRepository.findByOwner(user);
    }

    public void deleteMealPlan(Long id) {
        mealPlanRepository.deleteById(id);
    }

    public Optional<MealPlan> getMealPlanById(Long id) {
        return mealPlanRepository.findById(id);
    }

    public void setMeal(CreateMealRequestDto assignMealDto) {
        MealPlan mealPlan = mealPlanRepository.findById(assignMealDto.getDiaryEntryId()).orElseThrow(()->new MealPlanNotFoundException("Meal not found"));
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

}
