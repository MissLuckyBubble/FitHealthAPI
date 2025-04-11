package fit.health.fithealthapi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MealComponentDto {
    private Long id;
    private String name;
    private String type; // "RECIPE" or "FOOD_ITEM"
}
