package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class NutritionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "macronutrients_id", nullable = false)
    protected Macronutrients macronutrients;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    protected Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    protected Set<Allergen> allergens = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    protected Set<HealthConditionSuitability> healthConditionSuitabilities = new HashSet<>();

    @Column(nullable = false)
    protected boolean verifiedByAdmin = false;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"recipes", "preferences", "healthConditions", "allergens", "dietaryPreferences"})
    protected User owner;
}

