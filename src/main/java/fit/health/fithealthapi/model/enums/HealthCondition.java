package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthCondition {
    DIABETES("Diabetes"),
    HYPERTENSION("Hypertension"),
    HEART_DISEASE("Heart Disease"),
    KIDNEY_DISEASE("Kidney Disease"),
    OBESITY("Obesity"),
    GLUTEN_INTOLERANCE("Gluten Intolerance");

    private final String displayName;

    HealthCondition(String displayName) {
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
    public static HealthCondition fromString(String value) {
        for (HealthCondition condition : HealthCondition.values()) {
            if (condition.getDisplayName().equalsIgnoreCase(value)) {
                return condition;
            }
        }
        throw new IllegalArgumentException("Unknown health condition: " + value);
    }
}
