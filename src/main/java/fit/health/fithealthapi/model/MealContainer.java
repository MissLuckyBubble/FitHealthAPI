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
    @OneToOne
    protected Meal breakfast;

    @OneToOne
    protected Meal lunch;

    @OneToOne
    protected Meal dinner;

    @OneToOne
    protected Meal snack;

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
