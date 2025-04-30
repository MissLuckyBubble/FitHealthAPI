package fit.health.fithealthapi.model.dto.mealplan;

import fit.health.fithealthapi.model.dto.meal.MealShortDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MealPlanDetailsDTO extends MealPlanSummaryDTO {
    private int likes;
    private int dislikes;
    private MealShortDTO breakfast;
    private MealShortDTO lunch;
    private MealShortDTO dinner;
    private MealShortDTO snack;
}
