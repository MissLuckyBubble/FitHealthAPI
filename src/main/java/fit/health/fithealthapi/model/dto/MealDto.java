package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.RecipeType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class MealDto {
    private String name;
    private Set<RecipeType> recipeTypes;
    private List<MealItemDto> mealItems;
}
