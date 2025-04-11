package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.RecipeType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RecipeDTO {
    private String name;
    private String description;
    private Integer preparationTime;
    private Integer cookingTime;
    private Integer servingSize;
    private Set<RecipeType> recipeTypes;
    private List<RecipeIngredientDTO> ingredients;
}
