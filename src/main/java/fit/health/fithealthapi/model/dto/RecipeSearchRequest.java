package fit.health.fithealthapi.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RecipeSearchRequest {
    private String name;
    private List<String> dietaryPreferences = new ArrayList<>();
    private List<String> allergens = new ArrayList<>();
    private List<String> healthConditions = new ArrayList<>();
    private List<String> ingredientNames = new ArrayList<>();
    private Float minCalories;
    private Float maxCalories;
    private Float maxTotalTime;
}
