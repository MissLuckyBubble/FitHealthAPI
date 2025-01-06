package fit.health.fithealthapi.model.enums;

public enum Unit {
    GRAMS(1.0f),            // Base unit (1 gram = 1 gram)
    KILOGRAMS(1000.0f),     // 1 kilogram = 1000 grams
    MILLILITERS(1.0f),      // Assuming 1 milliliter = 1 gram (water-like density)
    LITERS(1000.0f),        // 1 liter = 1000 grams (water-like density)
    PIECES(50.0f),          // Default piece = 50 grams (can vary based on item)
    LARGE_EGG(60.0f),       // 1 large egg = 60 grams
    MEDIUM_EGG(50.0f),      // 1 medium egg = 50 grams
    SMALL_EGG(40.0f),       // 1 small egg = 40 grams
    CUPS(240.0f),           // 1 cup = 240 grams (general approximation)
    TABLESPOONS(15.0f),     // 1 tablespoon = 15 grams
    TEASPOONS(5.0f);        // 1 teaspoon = 5 grams

    private final float gramsEquivalent;

    Unit(float gramsEquivalent) {
        this.gramsEquivalent = gramsEquivalent;
    }

    public float getGramsEquivalent() {
        return gramsEquivalent;
    }

    public float convertToGrams(float quantity) {
        return quantity * gramsEquivalent;
    }
}
