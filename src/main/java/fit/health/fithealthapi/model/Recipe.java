package fit.health.fithealthapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
    private String id;
    private String recipeName;
    private int cookingTime;
    private int preparationTime;
    private int servingSize;
    private String description;
    private List<String> ingredients;
    private List<String> dietaryPreferences;
    private float caloriesPer100gram;
    private float fatContent;
    private float proteinContent;
    private float sugarContent;
    //private List<String> allergens;
}