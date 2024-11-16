package fit.health.fithealthapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MealPlan {
    private String id;
    String mealPlanName;
    List<String> recipes;
    float totalCalories;


}
