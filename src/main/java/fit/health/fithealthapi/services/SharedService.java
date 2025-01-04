package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.Gender;
import fit.health.fithealthapi.model.enums.HealthCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SharedService {
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
}
