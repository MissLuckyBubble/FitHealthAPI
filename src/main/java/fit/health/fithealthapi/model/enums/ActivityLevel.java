package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum ActivityLevel {
    SEDENTARY("Sedentary", 1.2),
    LIGHT("Light Activity", 1.375),
    MODERATE("Moderate Activity", 1.55),
    VERY_ACTIVE("Very Active", 1.725),
    SUPER_ACTIVE("Super Active", 1.9);

    private final String displayName;
    @Getter
    private final double multiplier;

    ActivityLevel(String displayName, double multiplier) {
        this.displayName = displayName;
        this.multiplier = multiplier;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ActivityLevel fromString(String value) {
        for (ActivityLevel level : ActivityLevel.values()) {
            if (level.getDisplayName().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown activity level: " + value);
    }
}
