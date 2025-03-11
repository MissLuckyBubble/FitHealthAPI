package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MealSearchDto {
    private String query;
    private RecipeType mealType;
    private Long userId;
    private Visibility visibility;
    private Boolean verifiedByAdmin;
    private Set<DietaryPreference> dietaryPreferences;
    private Set<HealthConditionSuitability> healthConditions;
    private Set<Allergen> excludeAllergens;
    private Float minCalories;
    private Float maxCalories;
    private Float minProtein;
    private Float maxProtein;
    private Float minFat;
    private Float maxFat;
    private String sortBy; // likes, date
    private String sortDirection; // asc or desc
}
