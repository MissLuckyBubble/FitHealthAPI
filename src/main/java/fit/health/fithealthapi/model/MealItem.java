package fit.health.fithealthapi.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
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
public class MealItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @ManyToOne
    @JoinColumn(name = "recipe_id") // Nullable if it's a FoodItem
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "food_item_id", nullable = true) // Nullable if it's a Recipe
    private FoodItem foodItem;

    @Column(nullable = true)
    private Float portionSize; // Servings (if recipe-based)

    @Column(nullable = true)
    private Float weightGrams; // Grams (if food-based)

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "macronutrients_id", nullable = false)
    private Macronutrients macronutrients;

    @Column(nullable = false)
    private boolean verifiedByAdmin = false;

    public void prePersist() {
        calculateMacronutrients();
        updateMealItemData();
    }

    @ElementCollection(fetch = FetchType.EAGER, targetClass = DietaryPreference.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_item_dietary_preferences", joinColumns = @JoinColumn(name = "meal_item_id"))
    @Column(name = "dietary_preference")
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Allergen.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_item_allergens", joinColumns = @JoinColumn(name = "meal_item_id"))
    @Column(name = "allergen")
    private Set<Allergen> allergens = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = HealthConditionSuitability.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_item_suitabilities", joinColumns = @JoinColumn(name = "meal_item_id"))
    @Column(name = "suitabilities")
    private Set<HealthConditionSuitability> healthConditionSuitabilities = new HashSet<>();

    public void updateMealItemData() {
        if (recipe != null) {
            getPreferences(recipe.getName(), recipe.isVerifiedByAdmin(), recipe.getDietaryPreferences(), recipe.getAllergens(), recipe.getHealthConditionSuitability());
        } else if (foodItem != null) {
            getPreferences(foodItem.getName(), foodItem.isVerifiedByAdmin(), foodItem.getDietaryPreferences(), foodItem.getAllergens(), foodItem.getHealthConditionSuitability());
        }
    }

    private void getPreferences(String name, boolean verifiedByAdmin, Set<DietaryPreference> dietaryPreferences, Set<Allergen> allergens, Set<HealthConditionSuitability> healthConditionSuitability) {
        this.name = name;
        this.verifiedByAdmin = verifiedByAdmin;
        this.dietaryPreferences = new HashSet<>(dietaryPreferences);
        this.allergens = new HashSet<>(allergens);
        this.healthConditionSuitabilities = new HashSet<>(healthConditionSuitability);
    }

    public void calculateMacronutrients() {
        Macronutrients calculatedMacronutrients = new Macronutrients();

        if (recipe != null) {
            if (portionSize != null) {
                calculatedMacronutrients.setCalories((recipe.getMacronutrients().getCalories() / recipe.getServingSize()) * portionSize);
                calculatedMacronutrients.setProtein((recipe.getMacronutrients().getProtein() / recipe.getServingSize()) * portionSize);
                calculatedMacronutrients.setFat((recipe.getMacronutrients().getFat() / recipe.getServingSize()) * portionSize);
                calculatedMacronutrients.setSugar((recipe.getMacronutrients().getSugar() / recipe.getServingSize()) * portionSize);
                calculatedMacronutrients.setSalt((recipe.getMacronutrients().getSalt() / recipe.getServingSize()) * portionSize);
            } else if (weightGrams != null && recipe.getTotalWeight() != null) {
                calculatedMacronutrients.setCalories((recipe.getMacronutrients().getCalories() / recipe.getTotalWeight()) * weightGrams);
                calculatedMacronutrients.setProtein((recipe.getMacronutrients().getProtein() / recipe.getTotalWeight()) * weightGrams);
                calculatedMacronutrients.setFat((recipe.getMacronutrients().getFat() / recipe.getTotalWeight()) * weightGrams);
                calculatedMacronutrients.setSugar((recipe.getMacronutrients().getSugar() / recipe.getTotalWeight()) * weightGrams);
                calculatedMacronutrients.setSalt((recipe.getMacronutrients().getSalt() / recipe.getTotalWeight()) * weightGrams);
            }
        } else if (foodItem != null && weightGrams != null) {
            calculatedMacronutrients.setCalories((foodItem.getMacronutrients().getCalories() / 100) * weightGrams);
            calculatedMacronutrients.setProtein((foodItem.getMacronutrients().getProtein() / 100) * weightGrams);
            calculatedMacronutrients.setFat((foodItem.getMacronutrients().getFat() / 100) * weightGrams);
            calculatedMacronutrients.setSugar((foodItem.getMacronutrients().getSugar() / 100) * weightGrams);
            calculatedMacronutrients.setSalt((foodItem.getMacronutrients().getSalt() / 100) * weightGrams);
        }

        this.macronutrients = calculatedMacronutrients;
    }
}