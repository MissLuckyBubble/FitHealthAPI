package fit.health.fithealthapi.model.dto.scoring;

import fit.health.fithealthapi.model.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ScoringUserDto {
    private Long id;
    private Goal goal;
    private float dailyCalorieGoal;

    private Set<Allergen> allergens;
    private Set<HealthCondition> healthConditions;
    private Set<DietaryPreference> dietaryPreferences;

    private List<Long> likedMealIds;
    private List<Long> dislikedMealIds;

    // Optional: track used meals for diversity penalty
    private Set<Long> usedMealIds;
}