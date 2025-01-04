package fit.health.fithealthapi.model;

import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
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

    @Column(nullable = false)
    private Float caloriesPer100g;

    @Column(nullable = false)
    private Float fatContent;

    @Column(nullable = false)
    private Float proteinContent;

    @Column(nullable = false)
    private Float sugarContent;

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
}
