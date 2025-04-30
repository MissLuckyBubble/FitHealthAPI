package fit.health.fithealthapi.model.dto.agents;

import fit.health.fithealthapi.model.dto.scoring.ScoringMealDto;
import fit.health.fithealthapi.model.dto.scoring.ScoringUserDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import jade.core.AID;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class MealPlanRequestContext {
    private final AID senderAID;
    private final ScoringUserDto user;
    private final int days;
    private final Set<RecipeType> requestedTypes;
    private final List<ScoringMealDto> candidateMeals;
}