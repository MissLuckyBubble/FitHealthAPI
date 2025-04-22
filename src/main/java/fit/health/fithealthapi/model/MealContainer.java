package fit.health.fithealthapi.model;

import fit.health.fithealthapi.interfeces.MealAggregator;
import fit.health.fithealthapi.model.enums.Visibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@MappedSuperclass
@Getter
@Setter
public abstract class MealContainer extends NutritionalProfile implements MealAggregator {
    @ManyToOne
    @JoinColumn(
            name = "breakfast_id",
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (breakfast_id) REFERENCES meals(id) ON DELETE SET NULL"
            )
    )
    private Meal breakfast;

    @ManyToOne
    @JoinColumn(
            name = "lunch_id",
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (lunch_id) REFERENCES meals(id) ON DELETE SET NULL"
            )
    )
    private Meal lunch;

    @ManyToOne
    @JoinColumn(
            name = "dinner_id",
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (dinner_id) REFERENCES meals(id) ON DELETE SET NULL"
            )
    )
    private Meal dinner;

    @ManyToOne
    @JoinColumn(
            name = "snack_id",
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (snack_id) REFERENCES meals(id) ON DELETE SET NULL"
            )
    )
    private Meal snack;
    @Override
    public List<Meal> getMeals() {
        return Arrays.asList(breakfast, lunch, dinner, snack);
    }

    @Override
    public Macronutrients getMacronutrients() {
        Macronutrients total = new Macronutrients();
        for (Meal meal : getMeals()) {
            if (meal != null) {
                total.add(meal.getMacronutrients());
            }
        }
        return total;
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;
}
