package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import fit.health.fithealthapi.model.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String ontologyLinkedName;

    private String description;

    @Column(nullable = false)
    private Integer preparationTime; // in minutes

    @Column(nullable = false)
    private Integer cookingTime; // in minutes

    @Column(nullable = false)
    private Integer servingSize;

    @Column(nullable = true)
    private Float totalWeight;

    @Column(nullable = true)
    private Float calories; // Total calories for the recipe

    @Column(nullable = true)
    private Float fatContent;

    @Column(nullable = true)
    private Float proteinContent;

    @Column(nullable = true)
    private Float saltContent;

    @Column(nullable = true)
    private Float sugarContent;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipeIngredient> ingredients = new HashSet<>();

    @ElementCollection(targetClass = DietaryPreference.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recipe_preferences", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "dietary_preference")
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @ElementCollection(targetClass = HealthConditionSuitability.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recipe_conditions", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "health_condition_suitability")
    private Set<HealthConditionSuitability> healthConditionSuitability = new HashSet<>();

    @ElementCollection(targetClass = Allergen.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recipe_allergens", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "allergens")
    private Set<Allergen> allergens = new HashSet<>();

    @ElementCollection(targetClass = RecipeType.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recipe_types", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "recipe_type")
    private Set<RecipeType> recipeTypes = new HashSet<>();

    public void setIngredients(Set<RecipeIngredient> ingredients) {
        this.ingredients.clear();
        if(ingredients != null) {
            for(RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipe(this);
            }
            this.ingredients.addAll(ingredients);
        }
    }

    @Column(nullable = false)
    private boolean verifiedByAdmin = false;

    public void checkAndUpdateVerification() {
        this.verifiedByAdmin = ingredients.stream()
                .allMatch(ingredient -> ingredient.getFoodItem().isVerifiedByAdmin());
    }
}
