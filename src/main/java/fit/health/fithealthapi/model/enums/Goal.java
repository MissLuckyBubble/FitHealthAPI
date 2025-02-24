package fit.health.fithealthapi.model.enums;

public enum Goal {
    FAT_LOSS_MODERATE(-500),
    FAT_LOSS_AGGRESSIVE(-750),
    MAINTAIN(0),
    GAIN_SLOW(250),
    GAIN_FAST(500);

    private final int calorieAdjustment;

    Goal(int calorieAdjustment) {
        this.calorieAdjustment = calorieAdjustment;
    }

    public int getCalorieAdjustment() {
        return calorieAdjustment;
    }
}
