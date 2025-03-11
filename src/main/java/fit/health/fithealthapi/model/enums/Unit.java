package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum Unit {
    GRAMS("Grams", 1.0f),             // Base unit (1 gram = 1 gram)
    KILOGRAMS("Kilograms", 1000.0f),  // 1 kilogram = 1000 grams
    MILLILITERS("Milliliters", 1.0f), // Assuming 1 milliliter = 1 gram (water-like density)
    LITERS("Liters", 1000.0f),        // 1 liter = 1000 grams (water-like density)
    PIECES("Pieces", 50.0f),          // Default piece = 50 grams (can vary based on item)
    LARGE_EGG("Large Egg", 60.0f),    // 1 large egg = 60 grams
    MEDIUM_EGG("Medium Egg", 50.0f),  // 1 medium egg = 50 grams
    SMALL_EGG("Small Egg", 40.0f),    // 1 small egg = 40 grams
    CUPS("Cups", 240.0f),             // 1 cup = 240 grams (general approximation)
    TABLESPOONS("Tablespoons", 15.0f), // 1 tablespoon = 15 grams
    TEASPOONS("Teaspoons", 5.0f);     // 1 teaspoon = 5 grams

    private final String displayName;
    @Getter
    private final float gramsEquivalent;

    // Constructor to set both display name and grams equivalent
    Unit(String displayName, float gramsEquivalent) {
        this.displayName = displayName;
        this.gramsEquivalent = gramsEquivalent;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;  // Return the human-readable display name
    }

    // Method to convert a quantity to grams
    public float convertToGrams(float quantity) {
        return quantity * gramsEquivalent;
    }

    public String toOntologyCase() {
        return displayName.replace(' ', '_');
    }

    @JsonCreator
    public static Unit fromString(String value) {
        for (Unit unit : Unit.values()) {
            if (unit.getDisplayName().equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown unit: " + value);
    }
}
