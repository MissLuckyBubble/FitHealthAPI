package fit.health.fithealthapi.model.dto.meal;

import fit.health.fithealthapi.model.enums.Unit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MealItemShortDTO {
    private Long id;
    private String name;
    private Float quantity;
    private Unit unit;
    private String type;
}
