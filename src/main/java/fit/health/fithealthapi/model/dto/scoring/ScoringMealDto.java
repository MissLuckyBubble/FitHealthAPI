package fit.health.fithealthapi.model.dto.scoring;

import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.RecipeType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ScoringMealDto {
    private Long id;
    private boolean verifiedByAdmin;
    private Macronutrients macronutrients;
    private Set<DietaryPreference> dietaryPreferences;
    private List<RecipeType> recipeTypes;
    private List<Long> componentIds;
}
