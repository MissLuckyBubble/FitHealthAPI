package fit.health.fithealthapi.mappers;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealItem;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.dto.meal.MealItemShortDTO;
import fit.health.fithealthapi.model.dto.meal.MealShortDTO;
import fit.health.fithealthapi.model.dto.meal.MealSummaryDTO;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;

import static fit.health.fithealthapi.mappers.UserMapper.toSimpleUserDTO;

public class MealMapper {
    public static MealSummaryDTO toMealSummaryDto(Meal meal){
        return new MealSummaryDTO(
                meal.getId(),
                meal.getName(),
                meal.getMacronutrients(),
                meal.getDietaryPreferences().stream().map(DietaryPreference::getDisplayName).toList(),
                meal.getAllergens().stream().map(Allergen::getDisplayName).toList(),
                meal.getHealthConditionSuitabilities().stream().map(HealthConditionSuitability::getDisplayName).toList(),
                toSimpleUserDTO(meal.getOwner()),
                meal.isVerifiedByAdmin(),
                meal.getMealItems().stream().map(MealMapper::toMealItemShortDTO).toList());
    }
    public static MealShortDTO toMealShortDTO(Meal meal) {
        if (meal == null) return null;
        MealShortDTO dto = new MealShortDTO();
        dto.setId(meal.getId());
        dto.setName(meal.getName());
        return dto;
    }
    public static MealItemShortDTO toMealItemShortDTO(MealItem item){
        MealItemShortDTO dto = new MealItemShortDTO();
        dto.setId(item.getComponent().getId());
        dto.setName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setQuantity(item.getQuantity());
        if(item.getComponent() instanceof FoodItem){
            dto.setType("FOOD_ITEM");
        }else if(item.getComponent() instanceof Recipe){
            dto.setType("RECIPE");
        }
        return dto;
    }
}
