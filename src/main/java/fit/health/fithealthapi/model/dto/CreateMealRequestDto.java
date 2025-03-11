package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.enums.RecipeType;
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
    private Long mealId;      // Nullable -> If set, means we are adding to an existing meal
    private String mealName;  // Only used if creating a new meal
    private Long recipeId;    // Nullable (if adding a recipe)
    private Long foodItemId;  // Nullable (if adding a food item)
    private Float portionSize;  // Nullable (for recipes)
    private Float weightGrams;  // Nullable (for food items)
    private RecipeType recipeType; // Enum: Breakfast, Lunch, Dinner, Snack
    private Long diaryEntryId; // Nullable (if null, create a new diary entry)
    private LocalDate date;
}
