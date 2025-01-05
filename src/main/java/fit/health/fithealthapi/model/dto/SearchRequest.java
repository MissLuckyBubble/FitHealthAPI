package fit.health.fithealthapi.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchRequest {
    private List<String> dietaryPreferences;
    private List<String> allergens;
    private List<String> healthConditions;
}

