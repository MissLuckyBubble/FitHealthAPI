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
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Visibility;
import fit.health.fithealthapi.repository.MealPlanRepository;
import fit.health.fithealthapi.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealRepository mealRepository;
    private final MealService mealService;

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

    public List<MealPlan> getUserMealPlans(User user) {
        return mealPlanRepository.findByUser(user);
    }

    public MealPlan updateMealPlan(MealPlan mealPlan) {
        return mealPlanRepository.save(mealPlan);
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
        updateVerificationStatus(mealPlan);
        updateMealPlanSuitabilityAndAllergens(mealPlan);
        updateDietaryPreferences(mealPlan);
        updateCaloriesLeft(mealPlan);
    }

    public void updateVerificationStatus(MealPlan mealPlan) {
        boolean verified = true;

        if (mealPlan.getBreakfast() != null && !mealPlan.getBreakfast().isVerifiedByAdmin()) {
            verified = false;
        }
        if (mealPlan.getLunch() != null && !mealPlan.getLunch().isVerifiedByAdmin()) {
            verified = false;
        }
        if (mealPlan.getDinner() != null && !mealPlan.getDinner().isVerifiedByAdmin()) {
            verified = false;
        }
        if (mealPlan.getSnack() != null && !mealPlan.getSnack().isVerifiedByAdmin()) {
            verified = false;
        }

        mealPlan.setVerifiedByAdmin(verified);
    }

    public void updateMealPlanSuitabilityAndAllergens(MealPlan mealPlan) {
        mealPlan.getAllergens().clear();

        if (mealPlan.getBreakfast() != null) {
            mealPlan.getAllergens().addAll(mealPlan.getBreakfast().getAllergens());
        }
        if (mealPlan.getLunch() != null) {
            mealPlan.getAllergens().addAll(mealPlan.getLunch().getAllergens());
        }
        if (mealPlan.getDinner() != null) {
            mealPlan.getAllergens().addAll(mealPlan.getDinner().getAllergens());
        }
        if (mealPlan.getSnack() != null) {
            mealPlan.getAllergens().addAll(mealPlan.getSnack().getAllergens());
        }

        Set<HealthConditionSuitability> commonConditions = new HashSet<>();
        boolean firstMeal = true;

        if (mealPlan.getBreakfast() != null) {
            commonConditions.addAll(mealPlan.getBreakfast().getHealthConditionSuitability());
            firstMeal = false;
        }
        if (mealPlan.getLunch() != null) {
            if (firstMeal) {
                commonConditions.addAll(mealPlan.getLunch().getHealthConditionSuitability());
                firstMeal = false;
            } else {
                commonConditions.retainAll(mealPlan.getLunch().getHealthConditionSuitability());
            }
        }
        if (mealPlan.getDinner() != null) {
            if (firstMeal) {
                commonConditions.addAll(mealPlan.getDinner().getHealthConditionSuitability());
                firstMeal = false;
            } else {
                commonConditions.retainAll(mealPlan.getDinner().getHealthConditionSuitability());
            }
        }
        if (mealPlan.getSnack() != null) {
            if (firstMeal) {
                commonConditions.addAll(mealPlan.getSnack().getHealthConditionSuitability());
            } else {
                commonConditions.retainAll(mealPlan.getSnack().getHealthConditionSuitability());
            }
        }

        mealPlan.setHealthConditionSuitability(commonConditions);
    }

    public void updateDietaryPreferences(MealPlan mealPlan) {
        Set<DietaryPreference> commonPreferences = new HashSet<>();
        boolean firstMeal = true;

        if (mealPlan.getBreakfast() != null) {
            commonPreferences.addAll(mealPlan.getBreakfast().getDietaryPreferences());
            firstMeal = false;
        }
        if (mealPlan.getLunch() != null) {
            if (firstMeal) {
                commonPreferences.addAll(mealPlan.getLunch().getDietaryPreferences());
                firstMeal = false;
            } else {
                commonPreferences.retainAll(mealPlan.getLunch().getDietaryPreferences());
            }
        }
        if (mealPlan.getDinner() != null) {
            if (firstMeal) {
                commonPreferences.addAll(mealPlan.getDinner().getDietaryPreferences());
                firstMeal = false;
            } else {
                commonPreferences.retainAll(mealPlan.getDinner().getDietaryPreferences());
            }
        }
        if (mealPlan.getSnack() != null) {
            if (firstMeal) {
                commonPreferences.addAll(mealPlan.getSnack().getDietaryPreferences());
            } else {
                commonPreferences.retainAll(mealPlan.getSnack().getDietaryPreferences());
            }
        }

        mealPlan.setDietaryPreferences(commonPreferences);
    }

    public void updateCaloriesLeft(MealPlan mealPlan) {
        if (mealPlan.getMacronutrients() == null) {
            mealPlan.setMacronutrients(new Macronutrients());
        }

        mealPlan.getMacronutrients().reset();

        if (mealPlan.getBreakfast() != null) {
            mealPlan.getMacronutrients().add(mealPlan.getBreakfast().getMacronutrients());
        }
        if (mealPlan.getLunch() != null) {
            mealPlan.getMacronutrients().add(mealPlan.getLunch().getMacronutrients());
        }
        if (mealPlan.getDinner() != null) {
            mealPlan.getMacronutrients().add(mealPlan.getDinner().getMacronutrients());
        }
        if (mealPlan.getSnack() != null) {
            mealPlan.getMacronutrients().add(mealPlan.getSnack().getMacronutrients());
        }
    }


    public MealPlan removeMeal(Long mealPlanId, RecipeType recipeType, User user) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId).orElseThrow(()->new MealPlanNotFoundException("Meal plan not found."));
        if(!mealPlan.getUser().getId().equals(user.getId())){
            throw new ForbiddenException("You are not allowed to remove this meal.");
        }
        switch (recipeType){
            case BREAKFAST -> mealPlan.setBreakfast(null);
            case LUNCH -> mealPlan.setLunch(null);
            case DINNER -> mealPlan.setDinner(null);
            case SNACK -> mealPlan.setSnack(null);
            default -> throw new IllegalStateException("Unexpected value: " + recipeType);
        }
        updateMealPlanData(mealPlan);
        return mealPlanRepository.save(mealPlan);
    }

    public List<MealPlan> searchMealPlan(MealSearchDto searchDto) {
        List<MealPlan> mealPlans = mealPlanRepository.findAll();

        if (searchDto.getQuery() != null && !searchDto.getQuery().isBlank()) {
            String searchText = searchDto.getQuery().toLowerCase();
            mealPlans = mealPlans.stream()
                    .filter(mealPlan ->
                            mealPlan.getName().toLowerCase().contains(searchText) ||
                                    mealService.searchInMeal(mealPlan.getBreakfast(), searchText) ||
                                    mealService.searchInMeal(mealPlan.getLunch(), searchText) ||
                                    mealService.searchInMeal(mealPlan.getDinner(), searchText) ||
                                    mealService.searchInMeal(mealPlan.getSnack(), searchText)
                    )
                    .toList();
        }

        if(searchDto.getUserId() != null){
            mealPlans = mealPlans.stream().filter(meal -> meal.getUser().getId().equals(searchDto.getUserId())).toList();
        }else {
            mealPlans = mealPlans.stream().filter(meal-> meal.getVisibility().equals(Visibility.PUBLIC)).toList();
        }

        if (searchDto.getDietaryPreferences() != null && !searchDto.getDietaryPreferences().isEmpty()) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getDietaryPreferences().containsAll(searchDto.getDietaryPreferences()))
                    .toList();
        }

        if (searchDto.getHealthConditions() != null && !searchDto.getHealthConditions().isEmpty()) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getHealthConditionSuitability().containsAll(searchDto.getHealthConditions()))
                    .toList();
        }

        if (searchDto.getExcludeAllergens() != null && !searchDto.getExcludeAllergens().isEmpty()) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getAllergens().stream().noneMatch(searchDto.getExcludeAllergens()::contains))
                    .toList();
        }

        if (searchDto.getMinCalories() != null) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getMacronutrients().getCalories() >= searchDto.getMinCalories())
                    .toList();
        }
        if (searchDto.getMaxCalories() != null) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getMacronutrients().getCalories() <= searchDto.getMaxCalories())
                    .toList();
        }

        if (searchDto.getMinProtein() != null) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getMacronutrients().getProtein() >= searchDto.getMinProtein())
                    .toList();
        }
        if (searchDto.getMaxProtein() != null) {
            mealPlans = mealPlans.stream()
                    .filter(meal -> meal.getMacronutrients().getProtein() <= searchDto.getMaxProtein())
                    .toList();
        }

        if (searchDto.getMinFat() != null) {
            mealPlans = mealPlans.stream()
                    .filter(mealPlan -> mealPlan.getMacronutrients().getFat() >= searchDto.getMinFat())
                    .toList();
        }
        if (searchDto.getMaxFat() != null) {
            mealPlans = mealPlans.stream()
                    .filter(mealPlan -> mealPlan.getMacronutrients().getFat() <= searchDto.getMaxFat())
                    .toList();
        }

        mealPlans = mealPlans.stream()
                .sorted(Comparator.comparing(MealPlan::isVerifiedByAdmin).reversed())
                .toList();

        if (searchDto.getSortBy() != null) {
            Comparator<MealPlan> comparator = null;

            switch (searchDto.getSortBy()) {
                case "likes" -> comparator = Comparator.comparingInt(meal -> meal.getUser().getFavoriteRecipes().size());
                case "date" -> comparator = Comparator.comparing(MealPlan::getId);
            }

            if (comparator != null) {
                if ("desc".equalsIgnoreCase(searchDto.getSortDirection())) {
                    comparator = comparator.reversed();
                }
                mealPlans = mealPlans.stream().sorted(comparator).toList();
            }
        }

        return mealPlans;
    }
}
