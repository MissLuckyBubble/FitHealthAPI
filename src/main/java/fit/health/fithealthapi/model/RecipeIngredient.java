package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fit.health.fithealthapi.model.enums.Unit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false,
            foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE"))
    @JsonIgnore
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "food_item_id", nullable = false,
            foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON DELETE CASCADE"))
    private FoodItem foodItem;

    @Column(nullable = false)
    private Float quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

}
