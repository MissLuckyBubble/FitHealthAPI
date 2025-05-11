package fit.health.fithealthapi.model.dto.dietary;

import fit.health.fithealthapi.model.dto.meal.MealSummaryDTO;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;

import java.util.Set;

public interface DietaryEntryDTO {
    Long getId();
    Float getCalories();
    Float getProtein();
    Float getFat();
    Float getSugar();
    Float getSalt();
    Set<String> getDietaryPreferences();
    Set<String> getAllergens();
    Set<String> getHealthConditionSuitabilities();
    SimpleUserDTO getOwner();
    MealSummaryDTO getBreakfast();
    MealSummaryDTO getLunch();
    MealSummaryDTO getDinner();
    MealSummaryDTO getSnack();
}