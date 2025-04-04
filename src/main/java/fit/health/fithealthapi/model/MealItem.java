package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.utils.MacronutrientCalculator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "meal_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MealItem extends NutritionalProfile{

    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @ManyToOne
    @JoinColumn(name = "component_id", nullable = false)
    private MealComponent component;

    @Column(nullable = true)
    private Float portionSize; // Servings (if recipe-based)

    @Column(nullable = true)
    private Float weightGrams; // Grams (if food-based)


    public void updateData() {
        calculateMacronutrients();
        updateMealItemData();
    }

    public void updateMealItemData() {
        if (component == null) return;

        this.name = component.getName();
        this.verifiedByAdmin = component.isVerifiedByAdmin();
        this.dietaryPreferences = new HashSet<>(component.getDietaryPreferences());
        this.allergens = new HashSet<>(component.getAllergens());
        this.healthConditionSuitabilities = new HashSet<>(component.getHealthConditionSuitabilities());
    }

    private void setPreferences(String name, boolean verifiedByAdmin, Set<DietaryPreference> dietaryPreferences, Set<Allergen> allergens, Set<HealthConditionSuitability> healthConditionSuitability) {
        this.name = name;
        this.verifiedByAdmin = verifiedByAdmin;
        this.dietaryPreferences = new HashSet<>(dietaryPreferences);
        this.allergens = new HashSet<>(allergens);
        this.healthConditionSuitabilities = new HashSet<>(healthConditionSuitability);
    }

    public void calculateMacronutrients() {
        if (component == null) return;

        this.macronutrients = MacronutrientCalculator.calculate(component, portionSize, weightGrams);
    }
}