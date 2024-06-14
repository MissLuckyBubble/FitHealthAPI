package fit.health.fithealthapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String password;
    private String birthDate;
    private float weightKG;
    private float goalWeight;
    private float heightCM;
    private float dailyCalorieGoal;
    private String gender;
    private List<String> dietaryPreferences;
    private List<String> healthConditions;
}
