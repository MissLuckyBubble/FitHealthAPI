package fit.health.fithealthapi.model.enums;

public enum ActivityLevel {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    VERY_ACTIVE(1.725),
    SUPER_ACTIVE(1.9);

    private final double multiplier;

    ActivityLevel(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
