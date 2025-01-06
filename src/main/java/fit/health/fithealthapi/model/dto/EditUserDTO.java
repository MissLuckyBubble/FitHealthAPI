package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EditUserDTO {
    private Long id;
    private String username;
    private String oldPassword;
    private String password;
    private String birthDate;
    private String email;
    private float weightKG;
    private float goalWeight;
    private float heightCM;
    private float dailyCalorieGoal;
    private String gender;

    private List<String> dietaryPreferences;

    private List<String> healthConditions;

    private List<Recipe> favoriteRecipes;
}
