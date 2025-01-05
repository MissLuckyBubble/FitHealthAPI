package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.services.SharedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SharedController {
    @Autowired
    private SharedService sharedService;

    @GetMapping("/preferences")
    public ResponseEntity<List<String>> getPreferences(){
        List<String> dietaryPreferences = sharedService.getDietaryPreferences();
        return ResponseEntity.ok(dietaryPreferences);
    }

    @GetMapping("/allergens")
    public ResponseEntity<List<String>> getAllergens(){
        List<String> allergens = sharedService.getAllergens();
        return ResponseEntity.ok(allergens);
    }

    @GetMapping("/health-conditions")
    public ResponseEntity<List<String>> getHealthConditions(){
        List<String> hc = sharedService.getAllHealthConditions();
        return ResponseEntity.ok(hc);
    }

    @GetMapping("/genders")
    public ResponseEntity<List<String>> getGenders(){
        List<String> g = sharedService.getGenders();
        return ResponseEntity.ok(g);
    }

    @GetMapping("/health-conditions-suitability")
    public ResponseEntity<List<String>> getHealthConditionsSuitability(){
        List<String> hc = sharedService.getAllHealthConditionsSuitability();
        return ResponseEntity.ok(hc);
    }
}
