package fit.health.fithealthapi.mappers;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanFlatData;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanSummaryDTO;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanDetailsDTO;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;

import static fit.health.fithealthapi.mappers.MealMapper.toMealShortDTO;
import static fit.health.fithealthapi.mappers.UserMapper.toSimpleUserDTO;

public class MealPlanMapper {

    public static MealPlanSummaryDTO toSummaryDTO(MealPlan mealPlan) {
        if (mealPlan == null) return null;

        MealPlanSummaryDTO dto = new MealPlanSummaryDTO();
        getMealPlanInfo(mealPlan, dto);
        return dto;
    }

    public static MealPlanSummaryDTO toSummaryDTO(MealPlanFlatData mealPlan) {
        if (mealPlan == null) return null;

        MealPlanSummaryDTO dto = new MealPlanSummaryDTO(
                mealPlan.getId(),
                mealPlan.getName(),
                copyMacros(mealPlan.getMacronutrients()),
                mealPlan.getDietaryPreferences().stream().map(DietaryPreference::getDisplayName).toList(),
                mealPlan.getAllergens().stream().map(Allergen::getDisplayName).toList(),
                mealPlan.getHealthConditionSuitabilities().stream().map(HealthConditionSuitability::getDisplayName).toList(),
                new SimpleUserDTO(mealPlan.getOwner().getId(),mealPlan.getOwner().getUsername()),
                mealPlan.isVerifiedByAdmin()
        );
        return dto;
    }

    private static Macronutrients copyMacros(Macronutrients source) {
        if (source == null) return null;
        Macronutrients copy = new Macronutrients();
        copy.setCalories(source.getCalories());
        copy.setSugar(source.getSugar());
        copy.setSalt(source.getSalt());
        copy.setFat(source.getFat());
        copy.setProtein(source.getProtein());
        return copy;
    }

    private static void getMealPlanInfo(MealPlan mealPlan, MealPlanSummaryDTO dto) {
        dto.setId(mealPlan.getId());
        dto.setName(mealPlan.getName());

        dto.setMacronutrients(copyMacros(mealPlan.getMacronutrients()));
        dto.setDietaryPreferences(mealPlan.getDietaryPreferences().stream().map(DietaryPreference::getDisplayName).toList());
        dto.setAllergens(mealPlan.getAllergens().stream().map(Allergen::getDisplayName).toList());
        dto.setHealthConditions(mealPlan.getHealthConditionSuitabilities().stream().map(HealthConditionSuitability::getDisplayName).toList());
        dto.setOwner(toSimpleUserDTO(mealPlan.getOwner()));
        dto.setVerifiedByAdmin(mealPlan.isVerifiedByAdmin());
    }

    public static MealPlanDetailsDTO toDetailsDTO(MealPlan mealPlan) {
        if (mealPlan == null) return null;

        MealPlanDetailsDTO dto = new MealPlanDetailsDTO();
        getMealPlanInfo(mealPlan, dto);

        if (mealPlan.getBreakfast() != null) {
            dto.setBreakfast(toMealShortDTO(mealPlan.getBreakfast()));
        }
        if (mealPlan.getLunch() != null) {
            dto.setLunch(toMealShortDTO(mealPlan.getLunch()));
        }
        if (mealPlan.getDinner() != null) {
            dto.setDinner(toMealShortDTO(mealPlan.getDinner()));
        }
        if (mealPlan.getSnack() != null) {
            dto.setSnack(toMealShortDTO(mealPlan.getSnack()));
        }
        return dto;
    }


}
