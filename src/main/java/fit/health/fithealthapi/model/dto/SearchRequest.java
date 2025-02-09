package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchRequest {
    private List<DietaryPreference> dietaryPreferences;
    private List<Allergen> allergens;
    private List<String> healthConditions;
    private List<HealthConditionSuitability> healthSuitabilities;
}

