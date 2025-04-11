package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.Unit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealItemDto {
    private Long componentId;
    private Float quantity;
    private Unit unit;
}
