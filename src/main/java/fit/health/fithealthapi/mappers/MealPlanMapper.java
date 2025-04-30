package fit.health.fithealthapi.mappers;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanSummaryDTO;
import fit.health.fithealthapi.model.dto.mealplan.MealPlanDetailsDTO;
import fit.health.fithealthapi.model.dto.meal.MealShortDTO;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;

public class MealPlanMapper {

    public static MealPlanSummaryDTO toSummaryDTO(MealPlan mealPlan) {
        if (mealPlan == null) return null;

        MealPlanSummaryDTO dto = new MealPlanSummaryDTO();
        getMealPlanInfo(mealPlan, dto);
        return dto;
    }

    private static void getMealPlanInfo(MealPlan mealPlan, MealPlanSummaryDTO dto) {
        dto.setId(mealPlan.getId());
        dto.setName(mealPlan.getName());
        Macronutrients newMacronutrients = new Macronutrients();
        newMacronutrients.setCalories(mealPlan.getMacronutrients().getCalories());
        newMacronutrients.setSugar(mealPlan.getMacronutrients().getSugar());
        newMacronutrients.setSalt(mealPlan.getMacronutrients().getSalt());
        newMacronutrients.setFat(mealPlan.getMacronutrients().getFat());
        newMacronutrients.setProtein(mealPlan.getMacronutrients().getProtein());

        System.out.println("MealPlan macronutrients: " + mealPlan.getMacronutrients());
        System.out.println("Calories in MealPlan: " + (mealPlan.getMacronutrients() != null ? mealPlan.getMacronutrients().getCalories() : "null"));


        dto.setMacronutrients(newMacronutrients);
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

    private static SimpleUserDTO toSimpleUserDTO(User user) {
        if (user == null) return null;
        SimpleUserDTO dto = new SimpleUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

    private static MealShortDTO toMealShortDTO(Meal meal) {
        if (meal == null) return null;
        MealShortDTO dto = new MealShortDTO();
        dto.setId(meal.getId());
        dto.setName(meal.getName());
        return dto;
    }
}
