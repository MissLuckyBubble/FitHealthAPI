package fit.health.fithealthapi.model;

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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    private String birthDate;
    private String email;
    private float weightKG;
    private float goalWeight;
    private float heightCM;
    private float dailyCalorieGoal;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = DietaryPreference.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_dietary_preferences", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "dietary_preference")
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = HealthCondition.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_health_conditions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "health_condition")
    private Set<HealthCondition> healthConditions = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Allergen.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_allergen", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "allergens")
    private Set<Allergen> allergens = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @ManyToMany
    @JoinTable(
            name = "user_favorite_recipes",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    private Set<Recipe> favoriteRecipes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_disliked_recipes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "disliked_recipe_id")
    private Set<Long> dislikedRecipes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_disliked_foods", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "disliked_food")
    private Set<Long> dislikedFoodItems = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_favorite_foods", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "favorite_food")
    private Set<Long> favoriteFoodItems = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_disliked_meals", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "disliked_meal")
    private Set<Long> dislikedMeals = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_favorite_meal", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "favorite_meal")
    private Set<Long> favoriteMeals = new HashSet<>();
}
