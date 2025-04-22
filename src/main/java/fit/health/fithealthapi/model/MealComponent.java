package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fit.health.fithealthapi.interfeces.NutritionalSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler" })
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type" // this adds a field like "type": "RECIPE"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Recipe.class, name = "RECIPE"),
        @JsonSubTypes.Type(value = FoodItem.class, name = "FOOD_ITEM")
})
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MealComponent extends NutritionalProfile implements NutritionalSource {
    protected String name;
    protected String ontologyLinkedName;

    @JsonIgnore
    @OneToMany(mappedBy = "component", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<MealItem> mealItems = new HashSet<>();
}
