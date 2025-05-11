package fit.health.fithealthapi.model.dto.mealplan;

import lombok.Getter;

@Getter
public class MealPlanSearchRow {
    public Long id;
    public String name;
    public Boolean verifiedByAdmin;
    public String visibility;

    public Long macronutrientsId;
    public Float calories;
    public Float protein;
    public Float fat;
    public Float sugar;
    public Float salt;

    public Long ownerId;
    public String username;

    public String[] dietaryPreferences;
    public String[] allergens;
    public String[] healthConditions;

    public MealPlanSearchRow(
            Long id,
            String name,
            Boolean verifiedByAdmin,
            String visibility,
            Long macronutrientsId,
            Float calories,
            Float protein,
            Float fat,
            Float sugar,
            Float salt,
            Long ownerId,
            String username,
            String dietaryPreferencesStr,
            String allergensStr,
            String healthConditionsStr
    ) {
        this.id = id;
        this.name = name;
        this.verifiedByAdmin = verifiedByAdmin;
        this.visibility = visibility;
        this.macronutrientsId = macronutrientsId;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.sugar = sugar;
        this.salt = salt;
        this.ownerId = ownerId;
        this.username = username;

        this.dietaryPreferences = dietaryPreferencesStr == null ? new String[0] : dietaryPreferencesStr.split(",");
        this.allergens = allergensStr == null ? new String[0] : allergensStr.split(",");
        this.healthConditions = healthConditionsStr == null ? new String[0] : healthConditionsStr.split(",");
    }
}

