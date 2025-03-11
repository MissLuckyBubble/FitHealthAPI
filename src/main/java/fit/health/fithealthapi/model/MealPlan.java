package fit.health.fithealthapi.model;

import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import fit.health.fithealthapi.model.enums.Visibility;
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
@Table(name = "meal_plans")
public class MealPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "macronutrients_id", nullable = false)
    private Macronutrients macronutrients;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "breakfast_id")
    private Meal breakfast;

    @OneToOne
    @JoinColumn(name = "lunch_id")
    private Meal lunch;

    @OneToOne
    @JoinColumn(name = "dinner_id")
    private Meal dinner;

    @OneToOne
    @JoinColumn(name = "snack_id")
    private Meal snack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = HealthConditionSuitability.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_conditions", joinColumns = @JoinColumn(name = "meal_id"))
    @Column(name = "health_condition_suitability")
    private Set<HealthConditionSuitability> healthConditionSuitability = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Allergen.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_plan_allergens", joinColumns = @JoinColumn(name = "meal_plan_id"))
    @Column(name = "allergen")
    private Set<Allergen> allergens = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = DietaryPreference.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "meal_plan_dietary_preferences", joinColumns = @JoinColumn(name = "meal_plan_id"))
    @Column(name = "dietary_preference")
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @Column(nullable = false)
    private boolean verifiedByAdmin = false;
}

