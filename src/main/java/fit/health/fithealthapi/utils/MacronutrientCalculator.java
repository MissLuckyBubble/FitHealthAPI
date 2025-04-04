package fit.health.fithealthapi.utils;

import fit.health.fithealthapi.interfeces.NutritionalSource;
import fit.health.fithealthapi.model.*;

public class MacronutrientCalculator {

    public static Macronutrients calculate(NutritionalSource source, Float portionSize, Float weightGrams) {
        Macronutrients base = source.getMacronutrients();
        Macronutrients result = new Macronutrients();

        if (source instanceof Recipe recipe) {
            if (portionSize != null && recipe.getServingSize() != null) {
                return scale(base, portionSize / recipe.getServingSize());
            } else if (weightGrams != null && recipe.getTotalWeight() != null) {
                return scale(base, weightGrams / recipe.getTotalWeight());
            }
        } else if (source instanceof FoodItem && weightGrams != null) {
            return scale(base, weightGrams / 100f); // per 100g base
        }

        return result;
    }

    private static Macronutrients scale(Macronutrients base, float ratio) {
        return new Macronutrients(null,
                base.getCalories() * ratio,
                base.getProtein() * ratio,
                base.getFat() * ratio,
                base.getSugar() * ratio,
                base.getSalt() * ratio);
    }
}
