package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.NutritionalProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MealComponentDto extends NutritionalProfile {
    private Long id;
    private String name;
    private String type; // "RECIPE" or "FOOD_ITEM"
}
