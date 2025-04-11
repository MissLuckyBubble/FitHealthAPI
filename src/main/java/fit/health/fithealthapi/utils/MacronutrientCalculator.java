package fit.health.fithealthapi.utils;

import fit.health.fithealthapi.interfeces.NutritionalSource;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.enums.Unit;

public class MacronutrientCalculator {

    public static Macronutrients calculate(NutritionalSource source, Float quantity, Unit unit) {
        if (source == null || quantity == null || unit == null) {
            return new Macronutrients();
        }

        Macronutrients base = source.getMacronutrients();
        if (base == null) {
            return new Macronutrients();
        }

        float multiplier = 0f;

        if (source instanceof Recipe recipe) {
            if (unit == Unit.SERVING && recipe.getServingSize() != null && recipe.getTotalWeight() != null) {
                float gramsPerServing = recipe.getTotalWeight() / recipe.getServingSize();
                float grams = gramsPerServing * quantity;
                multiplier = grams / recipe.getTotalWeight();
            } else if (recipe.getTotalWeight() != null) {
                float grams = unit.convertToGrams(quantity);
                multiplier = grams / recipe.getTotalWeight();
            }
        }

        else if (source instanceof FoodItem) {
            float grams = unit.convertToGrams(quantity);
            multiplier = grams / 100f; // 100g is base for FoodItems
        }

        return scale(base, multiplier);
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
