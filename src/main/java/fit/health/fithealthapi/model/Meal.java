package fit.health.fithealthapi.model;

import fit.health.fithealthapi.interfeces.MealAggregator;
import fit.health.fithealthapi.interfeces.NutritionalSource;
import fit.health.fithealthapi.model.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Meal extends NutritionalProfile implements NutritionalSource, MealAggregator {

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = RecipeType.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_types", joinColumns = @JoinColumn(name = "meal_id"))
    @Column(name = "meal_type")
    private Set<RecipeType> recipeTypes = new HashSet<>();

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<MealItem> mealItems = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    public void updateMealData() {
        recalculateMacronutrients();
        updateSuitabilityAndAllergens();
        updateDietaryPreferences();
        updateVerificationStatus();

        if ((getName() == null || getName().isBlank()) && getOwner() != null) {
            setName(getOwner().getUsername() + "'s Meal");
        }
    }

    public void recalculateMacronutrients() {
        if (this.macronutrients == null) {
            this.macronutrients = new Macronutrients();
        }
        this.macronutrients.reset();

        for (MealItem item : mealItems) {
            if (item.getMacronutrients() != null) {
                this.macronutrients.add(item.getMacronutrients());
            }
        }
    }


    public void updateSuitabilityAndAllergens() {
        this.allergens.clear();

        for (MealItem item : mealItems) {
            if (item.getAllergens() != null) {
                this.allergens.addAll(item.getAllergens());
            }
        }

        // Remove ALLERGEN_FREE if actual allergens exist
        if (!this.allergens.isEmpty()) {
            this.allergens.remove(Allergen.ALLERGEN_FREE);
        }

        // If after processing the set is still empty, explicitly mark as allergen-free
        if (this.allergens.isEmpty()) {
            this.allergens.add(Allergen.ALLERGEN_FREE);
        }


        Set<HealthConditionSuitability> intersection = new HashSet<>();
        boolean first = true;

        for (MealItem item : mealItems) {
            if (item.getHealthConditionSuitabilities() != null) {
                if (first) {
                    intersection.addAll(item.getHealthConditionSuitabilities());
                    first = false;
                } else {
                    intersection.retainAll(item.getHealthConditionSuitabilities());
                }
            }
        }

        this.healthConditionSuitabilities = intersection;
    }


    public void updateVerificationStatus() {
        this.verifiedByAdmin = mealItems.stream().allMatch(MealItem::isVerifiedByAdmin);
    }


    public void updateDietaryPreferences() {
        Set<DietaryPreference> common = new HashSet<>();
        boolean first = true;

        for (MealItem item : mealItems) {
            if (item.getDietaryPreferences() != null) {
                if (first) {
                    common.addAll(item.getDietaryPreferences());
                    first = false;
                } else {
                    common.retainAll(item.getDietaryPreferences());
                }
            }
        }

        this.dietaryPreferences = common;
    }

    /**
     * Meals inside a Meal are not applicable — return empty list.
     */
    @Override
    public List<Meal> getMeals() {
        return List.of();
    }

    public void setMealItems(Set<MealItem> mealItems) {
        this.mealItems = (mealItems != null) ? new HashSet<>(mealItems) : new HashSet<>();
    }
}
