package fit.health.fithealthapi.model.dto.meal;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MealSummaryDTO {
    private Long id;
    private String name;
    private Macronutrients macronutrients;
    private List<String> dietaryPreferences;
    private List<String> allergens;
    private List<String> healthConditions;
    private SimpleUserDTO owner;
    private boolean verifiedByAdmin;
    private List<MealItemShortDTO> mealItems;
}
