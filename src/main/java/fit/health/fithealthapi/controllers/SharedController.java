package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.enums.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SharedController {

    // ===================== Enum Endpoints =====================

    @GetMapping("/preferences")
    public ResponseEntity<List<DietaryPreference>> getPreferences() {
        return ResponseEntity.ok(List.of(DietaryPreference.values()));
    }

    @GetMapping("/allergens")
    public ResponseEntity<List<Allergen>> getAllergens() {
        return ResponseEntity.ok(List.of(Allergen.values()));
    }

    @GetMapping("/health-conditions")
    public ResponseEntity<List<HealthCondition>> getHealthConditions() {
        return ResponseEntity.ok(List.of(HealthCondition.values()));
    }

    @GetMapping("/genders")
    public ResponseEntity<List<Gender>> getGenders() {
        return ResponseEntity.ok(List.of(Gender.values()));
    }


    @GetMapping("/health-conditions-suitability")
    public ResponseEntity<List<HealthConditionSuitability>> getHealthConditionsSuitability() {
        return ResponseEntity.ok(List.of(HealthConditionSuitability.values()));
    }

    @GetMapping("/recipe-types")
    public ResponseEntity<List<RecipeType>> getRecipeTypes() {
        return ResponseEntity.ok(List.of(RecipeType.values()));
    }

    @GetMapping("/units")
    public ResponseEntity<List<Unit>> getUnits() {
        return ResponseEntity.ok(List.of(Unit.values()));
    }


    @GetMapping("/enums")
    public ResponseEntity<Map<String, List<?>>> getAllEnums() {
        return ResponseEntity.ok(Map.of(
                "preferences", List.of(DietaryPreference.values()),
                "allergens", List.of(Allergen.values()),
                "healthConditions", List.of(HealthCondition.values()),
                "genders", List.of(Gender.values()),
                "healthConditionSuitability", List.of(HealthConditionSuitability.values()),
                "recipeTypes", List.of(RecipeType.values()),
                "units", List.of(Unit.values()),
                "visibilityOptions", List.of(Visibility.values())
        ));
    }
}
