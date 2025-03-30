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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_items")
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String ontologyLinkedName;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "macronutrients_id", nullable = false)
    private Macronutrients macronutrients;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<RecipeIngredient> recipes = new HashSet<>();

    @ElementCollection(targetClass = Allergen.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "food_item_allergens", joinColumns = @JoinColumn(name = "food_item_id"))
    @Column(name = "allergen")
    private Set<Allergen> allergens = new HashSet<>();

    @ElementCollection(targetClass = DietaryPreference.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "food_item_preferences", joinColumns = @JoinColumn(name = "food_item_id"))
    @Column(name = "dietary_preference")
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @ElementCollection(targetClass = HealthConditionSuitability.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "food_item_HealthConditionSuitability", joinColumns = @JoinColumn(name = "food_item_id"))
    @Column(name = "HealthConditionSuitability")
    private Set<HealthConditionSuitability> healthConditionSuitability = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private boolean verifiedByAdmin = false;

}
