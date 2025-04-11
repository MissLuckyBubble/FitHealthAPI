package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.dto.MealComponentDto;
import fit.health.fithealthapi.repository.FoodItemRepository;
import fit.health.fithealthapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MealComponentService {

    private final FoodItemRepository foodItemRepository;
    private final RecipeRepository recipeRepository;

    public MealComponentService(FoodItemRepository foodItemRepository, RecipeRepository recipeRepository) {
        this.foodItemRepository = foodItemRepository;
        this.recipeRepository = recipeRepository;
    }

    public List<MealComponentDto> getAllComponents() {
        List<MealComponentDto> result = new ArrayList<>();

        for (FoodItem fi : foodItemRepository.findAll()) {
            result.add(new MealComponentDto(fi.getId(), fi.getName(), "FOOD_ITEM"));
        }

        for (Recipe recipe : recipeRepository.findAll()) {
            result.add(new MealComponentDto(recipe.getId(), recipe.getName(), "RECIPE"));
        }

        return result;
    }
}
