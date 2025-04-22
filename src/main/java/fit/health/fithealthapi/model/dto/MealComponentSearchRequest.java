package fit.health.fithealthapi.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.model.enums.Goal;

@Getter
@Setter
public class MealComponentSearchRequest {

    // Free-text search: name, description, or ingredients
    private String query;

    // Filters
    private List<DietaryPreference> dietaryPreferences = new ArrayList<>();
    private List<Allergen> allergens = new ArrayList<>();
    private List<HealthConditionSuitability> conditionSuitability = new ArrayList<>();
    private Float minCalories;
    private Float maxCalories;
    private Float minProtein;
    private Float maxProtein;
    private Float minFat;
    private Float maxFat;

    // Recipe-specific filters
    private List<RecipeType> recipeTypes = new ArrayList<>();
    private Float maxTotalTime;

    // Meta filters
    private Goal goal; // used for sorting calories differently
    private Boolean onlyRecipes;
    private Boolean onlyFoodItems;
    private Boolean verifiedOnly;
    private Long ownerId;

    // Sorting and limit
    private String sortBy;        // likes, date, calories, protein
    private String sortDirection; // asc or desc
    private Integer limit = 10;
}