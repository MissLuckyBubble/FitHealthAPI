package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Allergen {
    PEANUT("Peanut"),
    DAIRY("Dairy"),
    NUTS("Nuts"),
    SHELLFISH("Shellfish"),
    ALLERGEN_FREE("Allergen Free"),
    MEAT("Meat"),
    ANIMAL_PRODUCT("Animal Product"),
    GLUTEN("Gluten"),
    EGGS("Eggs");

    private final String displayName;

    Allergen(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String toOntologyCase() {
        return getDisplayName().replace(' ', '_'); // Replace underscores with space
    }

    @JsonCreator  // For converting back from string to enum
    public static Allergen fromString(String value) {
        for (Allergen allergen : Allergen.values()) {
            if (allergen.getDisplayName().equalsIgnoreCase(value)) {
                return allergen;
            }
        }
        throw new IllegalArgumentException("Unknown allergen value: " + value);
    }
}
