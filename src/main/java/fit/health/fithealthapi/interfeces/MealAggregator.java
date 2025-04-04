package fit.health.fithealthapi.interfeces;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Visibility;

import java.util.List;

public interface MealAggregator {
    List<Meal> getMeals();
    Macronutrients getMacronutrients();
    Visibility getVisibility();
    User getOwner();
}
