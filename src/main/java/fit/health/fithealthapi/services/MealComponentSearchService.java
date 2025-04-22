package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.dto.MealComponentSearchRequest;
import fit.health.fithealthapi.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.enums.*;
import fit.health.fithealthapi.repository.MealComponentRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MealComponentSearchService {

    private final MealComponentRepository mealComponentRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    public List<MealComponent> searchMealComponents(MealComponentSearchRequest request) {
        List<MealComponent> all = mealComponentRepository.findAll();

        Stream<MealComponent> stream = all.stream();

        if (Boolean.TRUE.equals(request.getOnlyRecipes())) {
            stream = stream.filter(mc -> mc instanceof Recipe);
        } else if (Boolean.TRUE.equals(request.getOnlyFoodItems())) {
            stream = stream.filter(mc -> mc instanceof FoodItem);
        }

        return stream
                .filter(mc -> matchesQuery(mc, request.getQuery()))
                .filter(mc -> matchesDietaryPreferences(mc, request.getDietaryPreferences()))
                .filter(mc -> excludesAllergens(mc, request.getAllergens()))
                .filter(mc -> matchesHealthConditions(mc, request.getConditionSuitability()))
                .filter(mc -> withinMacronutrientRange(mc, request))
                .filter(mc -> filterByVerified(mc, request.getVerifiedOnly()))
                .filter(mc -> filterByOwner(mc, request.getOwnerId()))
                .filter(mc -> recipeSpecificFilters(mc, request))
                .sorted(getMealComponentComparator(request))
                .limit(request.getLimit())
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(MealComponent mc, String query) {
        if (query == null || query.isBlank()) return true;
        String lower = query.toLowerCase();

        if (mc.getName() != null && mc.getName().toLowerCase().contains(lower)) return true;

        if (mc instanceof Recipe recipe) {
            if (recipe.getDescription() != null && recipe.getDescription().toLowerCase().contains(lower)) return true;
            return recipe.getIngredients().stream()
                    .map(i -> i.getFoodItem().getName().toLowerCase())
                    .anyMatch(name -> name.contains(lower));
        }

        return false;
    }

    private boolean matchesDietaryPreferences(MealComponent mc, List<DietaryPreference> preferences) {
        return preferences == null || mc.getDietaryPreferences().containsAll(preferences);
    }

    private boolean excludesAllergens(MealComponent mc, List<Allergen> allergens) {
        return allergens == null || Collections.disjoint(mc.getAllergens(), allergens);
    }

    private boolean matchesHealthConditions(MealComponent mc, List<HealthConditionSuitability> conditions) {
        return conditions == null || mc.getHealthConditionSuitabilities().containsAll(conditions);
    }

    private boolean withinMacronutrientRange(MealComponent mc, MealComponentSearchRequest req) {
        Macronutrients m = mc.getMacronutrients();
        return (req.getMinCalories() == null || m.getCalories() >= req.getMinCalories()) &&
                (req.getMaxCalories() == null || m.getCalories() <= req.getMaxCalories()) &&
                (req.getMinProtein() == null || m.getProtein() >= req.getMinProtein()) &&
                (req.getMaxProtein() == null || m.getProtein() <= req.getMaxProtein()) &&
                (req.getMinFat() == null || m.getFat() >= req.getMinFat()) &&
                (req.getMaxFat() == null || m.getFat() <= req.getMaxFat());
    }

    private boolean filterByVerified(MealComponent mc, Boolean verifiedOnly) {
        return verifiedOnly == null || !verifiedOnly || mc.isVerifiedByAdmin();
    }

    private boolean filterByOwner(MealComponent mc, Long ownerId) {
        return ownerId == null || (mc.getOwner() != null && mc.getOwner().getId().equals(ownerId));
    }

    private boolean recipeSpecificFilters(MealComponent mc, MealComponentSearchRequest req) {
        if (!(mc instanceof Recipe recipe)) return true;

        boolean timeOk = req.getMaxTotalTime() == null ||
                (recipe.getPreparationTime() + recipe.getCookingTime()) <= req.getMaxTotalTime();

        boolean typesOk = req.getRecipeTypes() == null || req.getRecipeTypes().isEmpty()
                || !Collections.disjoint(recipe.getRecipeTypes(), req.getRecipeTypes());

        return timeOk && typesOk;
    }

    private Comparator<MealComponent> getMealComponentComparator(MealComponentSearchRequest req) {
        Comparator<MealComponent> comparator = Comparator.comparing(mc -> 0); // default

        switch (Optional.ofNullable(req.getSortBy()).orElse("").toLowerCase()) {
            case "likes" -> comparator = Comparator.comparingInt(this::getFavoriteCount);
            case "date" -> comparator = Comparator.comparing(MealComponent::getId);
            case "calories" -> comparator = Comparator.comparing(mc -> mc.getMacronutrients().getCalories(), Comparator.nullsLast(Float::compare));
            case "protein" -> comparator = Comparator.comparing(mc -> mc.getMacronutrients().getProtein(), Comparator.nullsLast(Float::compare));
        }

        if ("desc".equalsIgnoreCase(req.getSortDirection())) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    private int getFavoriteCount(MealComponent mc) {
        UserItemType type = mc instanceof Recipe ? UserItemType.RECIPE :
                mc instanceof FoodItem ? UserItemType.FOOD_ITEM : null;

        return type != null
                ? userPreferenceRepository.countByItemTypeAndItemIdAndPreferenceType(type, mc.getId(), PreferenceType.LIKE)
                : 0;
    }
}