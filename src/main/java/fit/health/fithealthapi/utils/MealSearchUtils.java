package fit.health.fithealthapi.utils;

import fit.health.fithealthapi.interfeces.MealAggregator;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.MealSearchDto;
import fit.health.fithealthapi.model.enums.PreferenceType;
import fit.health.fithealthapi.model.enums.UserItemType;
import fit.health.fithealthapi.model.enums.Visibility;

import java.util.Comparator;
import java.util.List;

public class MealSearchUtils {

    public static <T extends MealAggregator> List<T> filterMeals(List<T> meals, MealSearchDto dto) {
        return meals.stream()
                .filter(meal -> filterByQuery(meal, dto.getQuery()))
                .filter(meal -> filterByUser(meal, dto.getOwnerId()))
                .filter(meal -> filterByVisibility(meal, dto.getVisibility()))
                .filter(meal -> filterByPreferences(meal, dto))
                .filter(meal -> filterByNutrients(meal, dto))
                .filter(meal-> filterByVerifiedOnly(meal,dto.getVerifiedOnly()))
                .sorted(sort(dto))
                .toList();
    }

    private static boolean filterByQuery(MealAggregator aggregator, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase();
        if (aggregator instanceof Meal meal) {
            return meal.getName().toLowerCase().contains(q)
                    || meal.getMealItems().stream().anyMatch(item ->
                    item.getName() != null && item.getName().toLowerCase().contains(q));
        } else if (aggregator instanceof MealPlan plan) {
            return plan.getName().toLowerCase().contains(q);
        }
        return false;
    }

    private static boolean filterByUser(MealAggregator item, Long userId) {
        if (userId == null) return true;
        return item.getOwner() != null && userId.equals(item.getOwner().getId());
    }

    private static boolean filterByVisibility(MealAggregator item, Visibility visibility) {
        if (visibility == null) return true;
        return item.getVisibility().equals(visibility);
    }

    private static boolean filterByPreferences(MealAggregator meal, MealSearchDto dto) {
        if (dto.getDietaryPreferences() != null && !dto.getDietaryPreferences().isEmpty()) {
            if (meal instanceof NutritionalProfile profile) {
                if (!profile.getDietaryPreferences().containsAll(dto.getDietaryPreferences())) return false;
            }
        }

        if (dto.getHealthConditions() != null && !dto.getHealthConditions().isEmpty()) {
            if (meal instanceof NutritionalProfile profile) {
                if (!profile.getHealthConditionSuitabilities().containsAll(dto.getHealthConditions())) return false;
            }
        }

        if (dto.getExcludeAllergens() != null && !dto.getExcludeAllergens().isEmpty()) {
            if (meal instanceof NutritionalProfile profile) {
                if (profile.getAllergens().stream().anyMatch(dto.getExcludeAllergens()::contains)) return false;
            }
        }

        return true;
    }

    private static boolean filterByNutrients(MealAggregator meal, MealSearchDto dto) {
        Macronutrients macros = meal.getMacronutrients();
        if (dto.getMinCalories() != null && macros.getCalories() < dto.getMinCalories()) return false;
        if (dto.getMaxCalories() != null && macros.getCalories() > dto.getMaxCalories()) return false;
        if (dto.getMinProtein() != null && macros.getProtein() < dto.getMinProtein()) return false;
        if (dto.getMaxProtein() != null && macros.getProtein() > dto.getMaxProtein()) return false;
        if (dto.getMinFat() != null && macros.getFat() < dto.getMinFat()) return false;
        if (dto.getMaxFat() != null && macros.getFat() > dto.getMaxFat()) return false;
        return true;
    }

    private static Comparator<MealAggregator> sort(MealSearchDto dto) {
        Comparator<MealAggregator> comparator = Comparator.comparing((MealAggregator m) -> false); // default

        if ("likes".equalsIgnoreCase(dto.getSortBy())) {
            comparator = Comparator.comparingInt(m ->{
                User owner = m.getOwner();
                if (owner == null || owner.getPreferences() == null) return 0;
                return (int) owner.getPreferences().stream()
                        .filter(p->p.getPreferenceType() == PreferenceType.LIKE)
                        .filter(p-> {
                            if(m instanceof Meal){
                                return p.getItemType() == UserItemType.MEAL
                                        && p.getItemId().equals(((Meal)m).getId());
                            }else if (m instanceof Recipe) {
                                return p.getItemType() == UserItemType.RECIPE && p.getItemId().equals(((Recipe) m).getId());
                            }
                            return false;
                        }).count();
            });
        } else if ("date".equalsIgnoreCase(dto.getSortBy())) {
            comparator = Comparator.comparing(m -> ((NutritionalProfile) m).getId());
        }

        if ("desc".equalsIgnoreCase(dto.getSortDirection())) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    private static boolean filterByVerifiedOnly(MealAggregator meal, Boolean verifiedOnly) {
        if (verifiedOnly == null || !verifiedOnly) return true;
        if (meal instanceof NutritionalProfile profile) {
            return profile.isVerifiedByAdmin();
        }
        return true;
    }

}
