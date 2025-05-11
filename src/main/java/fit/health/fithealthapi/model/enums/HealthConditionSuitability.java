package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthConditionSuitability {
    DIABETES_SAFE("Diabetes Safe"),
    HYPERTENSION_SAFE("Hypertension Safe"),
    HEART_DISEASE_SAFE("Heart Disease Safe"),
    KIDNEY_DISEASE_SAFE("Kidney Disease Safe"),
    OBESITY_SAFE("Obesity Safe"),
    GLUTEN_INTOLERANCE_SAFE("Gluten Intolerance Safe");

    private final String displayName;

    HealthConditionSuitability(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String toOntologyCase() {
        return displayName.replace(' ', '_');
    }

    @JsonCreator
    public static HealthConditionSuitability fromString(String value) {
        for (HealthConditionSuitability suitability : HealthConditionSuitability.values()) {
            if (suitability.getDisplayName().equalsIgnoreCase(value) || suitability.name().equalsIgnoreCase(value)) {
                return suitability;
            }
        }
        throw new IllegalArgumentException("Unknown health condition suitability: " + value);
    }
}
