package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Visibility {
    PRIVATE("Private"),
    PUBLIC("Public");
    private final String displayName;
    Visibility(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Visibility fromString(String value) {
        for (Visibility visibility : Visibility.values()) {
            if (visibility.getDisplayName().equalsIgnoreCase(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown Visibility: " + value);
    }
}