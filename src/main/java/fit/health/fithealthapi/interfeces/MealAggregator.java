package fit.health.fithealthapi.interfeces;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.Visibility;

import java.util.Collection;
import java.util.List;

public interface MealAggregator {
    List<Meal> getMeals();
    Macronutrients getMacronutrients();
    Visibility getVisibility();
    User getOwner();

    Collection<DietaryPreference> getDietaryPreferences();
    Collection<HealthConditionSuitability> getHealthConditionSuitabilities();
    Collection<Allergen> getAllergens();

    Long getId();

    boolean isVerifiedByAdmin();
}
