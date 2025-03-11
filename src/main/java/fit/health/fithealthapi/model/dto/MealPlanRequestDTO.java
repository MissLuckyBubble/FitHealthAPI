package fit.health.fithealthapi.model.dto;
import fit.health.fithealthapi.model.enums.RecipeType;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class MealPlanRequestDTO {
    private Set<RecipeType> recipeTypes;
    private int days = 1;
}
