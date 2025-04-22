package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum Goal {
    FAT_LOSS_MODERATE("Fat Loss (Moderate)", -500),
    FAT_LOSS_AGGRESSIVE("Fat Loss (Aggressive)", -750),
    MAINTAIN("Maintain Weight", 0),
    GAIN_SLOW("Muscle Gain (Slow)", 250),
    GAIN_FAST("Muscle Gain (Fast)", 500);

    private final String displayName;
    @Getter
    private final int calorieAdjustment;

    Goal(String displayName, int calorieAdjustment) {
        this.displayName = displayName;
        this.calorieAdjustment = calorieAdjustment;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Goal fromString(String value) {
        for (Goal goal : Goal.values()) {
            if (goal.getDisplayName().equalsIgnoreCase(value)) {
                return goal;
            }
        }
        throw new IllegalArgumentException("Unknown goal: " + value);
    }
}
