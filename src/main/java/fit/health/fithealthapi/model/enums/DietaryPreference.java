package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DietaryPreference {
    CARNIVORE("Carnivore"),
    FAT_FREE("Fat Free"),
    GLUTEN_FREE("Gluten Free"),
    HIGH_CALORIE("High Calorie"),
    HIGH_FAT("High Fat"),
    HIGH_PROTEIN("High Protein"),
    HIGH_SUGAR("High Sugar"),
    LACTOSE_FREE("Lactose Free"),
    LOW_CALORIE("Low Calorie"),
    LOW_FAT("Low Fat"),
    LOW_SUGAR("Low Sugar"),
    VEGAN("Vegan"),
    VEGETARIAN("Vegetarian");

    private final String displayName;

    // Constructor to set the display name for each enum constant
    DietaryPreference(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;  // Return the human-readable display name
    }

    public String toOntologyCase() {
        // Convert the display name to PascalCase with underscores
        return displayName.replace(' ', '_');
    }

    @JsonCreator
    public static DietaryPreference fromString(String value) {
        for (DietaryPreference preference : DietaryPreference.values()) {
            if (preference.getDisplayName().equalsIgnoreCase(value) || preference.name().equalsIgnoreCase(value)) {
                return preference;
            }
        }
        throw new IllegalArgumentException("Unknown dietary preference: " + value);
    }
}
