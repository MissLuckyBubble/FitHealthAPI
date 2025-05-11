package fit.health.fithealthapi.model.dto.mealplan;

import fit.health.fithealthapi.interfeces.MealAggregator;
import fit.health.fithealthapi.model.Macronutrients;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanFlatData implements MealAggregator {
    private Long id;
    private String name;
    private Macronutrients macronutrients;
    private Set<DietaryPreference> dietaryPreferences;
    private Set<Allergen> allergens;
    private Set<HealthConditionSuitability> healthConditionSuitabilities;
    private User owner;
    private boolean verifiedByAdmin;
    private Visibility visibility;

    public MealPlanFlatData(MealPlanSearchRow row) {
        this.id = row.getId();
        this.name = row.getName();
        this.macronutrients = new Macronutrients(
                row.getMacronutrientsId(),
                row.getCalories(),
                row.getProtein(),
                row.getFat(),
                row.getSugar(),
                row.getSalt()
        );
        this.dietaryPreferences = Arrays.stream(row.getDietaryPreferences()).map(DietaryPreference::fromString).collect(Collectors.toSet());
        this.allergens = Arrays.stream(row.getAllergens()).map(Allergen::fromString).collect(Collectors.toSet());
        this.healthConditionSuitabilities = Arrays.stream(row.getHealthConditions()).map(HealthConditionSuitability::fromString).collect(Collectors.toSet());
        this.owner = new User();
        owner.setId(row.getOwnerId());
        owner.setUsername(row.getUsername());
        this.verifiedByAdmin = row.getVerifiedByAdmin();
        this.visibility = Visibility.fromString(row.getVisibility());
    }

    @Override
    public List<Meal> getMeals() {
        return List.of(); // override as needed
    }
}
