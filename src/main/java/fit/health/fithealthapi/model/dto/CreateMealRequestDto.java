package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Unit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMealRequestDto {
    private Long mealId;           // Nullable → add to existing meal
    private String mealName;       // Used only for new meal
    private Long componentId;      // Unified: recipe or foodItem ID
    private String componentType;  // "RECIPE" or "FOOD_ITEM"
    private Float quantity;        // Unified amount
    private Unit unit;             // Unified unit
    private RecipeType recipeType; // Enum: Breakfast, Lunch, Dinner, Snack
    private Long diaryEntryId;     // If null → create new diary
    private LocalDate date;
}
