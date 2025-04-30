package fit.health.fithealthapi.model.dto.agents;

import fit.health.fithealthapi.model.dto.scoring.ScoringMealDto;
import fit.health.fithealthapi.model.dto.scoring.ScoringUserDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MealScoringRequest {
    private ScoringUserDto user;
    private List<ScoringMealDto> meals;
    private List<RecipeType> requestedTypes;
    private int daysToGenerate;
}
