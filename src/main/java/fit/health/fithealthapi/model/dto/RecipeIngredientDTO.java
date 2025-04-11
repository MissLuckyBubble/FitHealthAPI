package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.Unit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipeIngredientDTO {
    private Long foodItem; // ID
    private Float quantity;
    private Unit unit;
}
