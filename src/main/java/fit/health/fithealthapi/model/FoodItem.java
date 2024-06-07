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
public class FoodItem {
    private String id;
    private String foodName;
    private float caloriesPer100gram;
    private float fatContent;
    private float proteinContent;
    private float sugarContent;
    private List<String> dietaryPreferences;
    private List<String> allergens;

}
