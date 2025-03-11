package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RecipeSearchRequest {
    private String name;
    private List<DietaryPreference> dietaryPreferences = new ArrayList<>();
    private List<Allergen> allergens = new ArrayList<>();
    private List<String> healthConditions = new ArrayList<>();
    private List<HealthConditionSuitability> conditionSuitability = new ArrayList<>();
    private List<String> ingredientNames = new ArrayList<>();
    private List<RecipeType> recipeTypes = new ArrayList<>();
    private Float minCalories;
    private Float maxCalories;
    private Float maxTotalTime;
    private Integer limit = 10;
    private Goal goal;
}
