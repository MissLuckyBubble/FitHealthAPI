package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
@Getter
public class InferredPreferences {
    private Set<DietaryPreference> dietaryPreferences;
    private Set<HealthConditionSuitability> healthConditionSuitabilities;
}
