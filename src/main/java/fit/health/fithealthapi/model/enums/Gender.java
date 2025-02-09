package fit.health.fithealthapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    FEMALE("Female"),
    MALE("Male"),
    PREFER_NOT_TO_SAY("Prefer Not to Say");

    private final String displayName;

    // Constructor to set the display name for each enum constant
    Gender(String displayName) {
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
    public static Gender fromString(String value) {
        for (Gender gender : Gender.values()) {
            if (gender.getDisplayName().equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + value);
    }
}
