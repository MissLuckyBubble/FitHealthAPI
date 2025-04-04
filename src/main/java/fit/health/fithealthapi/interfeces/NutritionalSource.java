package fit.health.fithealthapi.interfeces;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;

import java.util.Set;

public interface NutritionalSource {
    String getName();
    boolean isVerifiedByAdmin();
    Set<DietaryPreference> getDietaryPreferences();
    Set<Allergen> getAllergens();
    Set<HealthConditionSuitability> getHealthConditionSuitabilities();
    Macronutrients getMacronutrients();
}
