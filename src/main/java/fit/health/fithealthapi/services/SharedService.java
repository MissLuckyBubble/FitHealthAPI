package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SharedService {
    @Autowired
    OntologyService ontologyService;

    public String convertToPascalCase(String input) {
        if(input == null || input.isBlank()){
            return input;
        }
        StringBuilder result = new StringBuilder();
        boolean convertNext = true;
        for(char c : input.toCharArray()){
            if(Character.isSpaceChar(c) || c == '_' || c == '-' || c == '.'){
                convertNext = true;
            } else if (convertNext) {
                c = Character.toUpperCase(c);
                convertNext = false;
            }else {
                c = Character.toLowerCase(c);
            }
            result.append(c);
        }
        return result.toString();
    }

    public List<String> getDietaryPreferences() {
        List<String> result = new ArrayList<>();
        for(DietaryPreference preference : DietaryPreference.values()){
            result.add(convertToPascalCase(preference.toString()).replace("_"," "));
        }
        return result;
    }
    public List<String> getAllergens(){
        List<String> result = new ArrayList<>();
        for(Allergen allergen : Allergen.values()){
            result.add(convertToPascalCase(allergen.toString()).replace("_"," "));
        }
        return result;
    }
    public List<String> getAllHealthConditions(){
        List<String> result = new ArrayList<>();
        for(HealthCondition hc : HealthCondition.values()){
            result.add(convertToPascalCase(hc.toString()).replace("_"," "));
        }
        return result;
    }
    public List<String> getGenders(){
        List<String> result = new ArrayList<>();
        for (Gender gender : Gender.values()) {
            result.add(convertToPascalCase(gender.toString()).replace("_"," "));
        }
        return result;
    }

    HealthConditionSuitability mapToSuitability(HealthCondition condition) {
        return switch (condition) {
            case DIABETES -> HealthConditionSuitability.DIABETES_SAFE;
            case HYPERTENSION -> HealthConditionSuitability.HYPERTENSION_SAFE;
            case HEART_DISEASE -> HealthConditionSuitability.HEART_DISEASE_SAFE;
            case KIDNEY_DISEASE -> HealthConditionSuitability.KIDNEY_DISEASE_SAFE;
            case OBESITY -> HealthConditionSuitability.OBESITY_SAFE;
            case GLUTEN_INTOLERANCE -> HealthConditionSuitability.GLUTEN_INTOLERANCE_SAFE;
            default -> throw new IllegalArgumentException("Unknown condition: " + condition);
        };
    }
    public List<String> getAllHealthConditionsSuitability(){
        List<String> result = new ArrayList<>();
        for(HealthConditionSuitability hc : HealthConditionSuitability.values()){
            result.add(convertToPascalCase(hc.toString()).replace("_"," "));
        }
        return result;
    }

    public List<DietaryPreference> convertToDietaryPreferences(List<String> preferences) {
        List<DietaryPreference> dietaryPreferences = new ArrayList<>();
        for (String preference : preferences) {
            String normalizedPreference = preference.toUpperCase().replace(" ", "_");
            if(ontologyService.isDietaryPreference(normalizedPreference)){
                dietaryPreferences.add(DietaryPreference.valueOf(normalizedPreference));
            }
        }
        return dietaryPreferences;
    }

    public List<Allergen> convertToAllergens(List<String> allergens) {
        List<Allergen> allergenEnums = new ArrayList<>();
        for (String allergen : allergens) {
            String normalizedAllergen = allergen.toUpperCase().replace(" ", "_");
            if(ontologyService.isAllergen(normalizedAllergen)){
                allergenEnums.add(Allergen.valueOf(normalizedAllergen));
            }
        }
        return allergenEnums;
    }

    public List<HealthConditionSuitability> convertToHealthConditionSuitability(List<String> preferences) {
        List<HealthConditionSuitability> healthConditionSuitabilities = new ArrayList<>();
        for (String preference : preferences) {
            String normalizedPreference = preference.toUpperCase().replace(" ", "_");
            if(ontologyService.isHealthConditionSuitability(normalizedPreference)){
                healthConditionSuitabilities.add(HealthConditionSuitability.valueOf(normalizedPreference));
            }else if(ontologyService.isHealthCondition(normalizedPreference)){
                healthConditionSuitabilities.add(mapToSuitability(HealthCondition.valueOf(normalizedPreference)));
            }
        }
        return healthConditionSuitabilities;
    }

}
