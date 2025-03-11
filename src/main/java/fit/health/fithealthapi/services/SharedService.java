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

    public String convertToOntoCase(String input) {
        if(input == null || input.isBlank()){
            return input;
        }
        return convertToPascalCase(input).replaceAll("\\s+", "_");
    }

}
