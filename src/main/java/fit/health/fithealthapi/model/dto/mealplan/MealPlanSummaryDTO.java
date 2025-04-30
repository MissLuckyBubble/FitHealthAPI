package fit.health.fithealthapi.model.dto.mealplan;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MealPlanSummaryDTO {
    private Long id;
    private String name;
    private Macronutrients macronutrients;
    private List<String> dietaryPreferences;
    private List<String> allergens;
    private List<String> healthConditions;
    private SimpleUserDTO owner;
    private boolean verifiedByAdmin;
}

