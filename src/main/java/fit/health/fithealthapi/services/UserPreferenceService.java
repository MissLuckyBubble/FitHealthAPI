package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.InvalidRequestException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.UserPreference;
import fit.health.fithealthapi.model.enums.PreferenceType;
import fit.health.fithealthapi.model.enums.UserItemType;
import fit.health.fithealthapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final RecipeRepository recipeRepository;
    private final MealRepository mealRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final MealComponentRepository mealComponentRepository;

    public UserPreference like(User user, UserItemType type, Long itemId) {
        return saveOrUpdate(user, type, itemId, PreferenceType.LIKE);
    }

    public UserPreference dislike(User user, UserItemType type, Long itemId) {
        return saveOrUpdate(user, type, itemId, PreferenceType.DISLIKE);
    }

    public List<UserPreference> getLikedItemsByType(User user, UserItemType type) {
        return preferenceRepository.findByUserAndPreferenceTypeAndItemType(user, PreferenceType.LIKE, type);
    }

    public List<UserPreference> getLikedItemsByType(Long userId, UserItemType type) {
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found"));
        return getLikedItemsByType(user, type);
    }

    public List<UserPreference> getDislikedItemsByType(User user, UserItemType type) {
        return preferenceRepository.findByUserAndPreferenceTypeAndItemType(user, PreferenceType.DISLIKE, type);
    }

    public List<UserPreference> getDislikedItemsByType(Long userId, UserItemType type) {
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found"));
        return getDislikedItemsByType(user, type);
    }

    private UserPreference saveOrUpdate(User user, UserItemType type, Long itemId, PreferenceType preferenceType) {
        if (user == null || type == null || itemId == null) {
            throw new InvalidRequestException("User, item type, and item ID must be provided.");
        }
        if (!doesItemExist(type, itemId)) {
            throw new NotFoundException("Item of type " + type + " with ID " + itemId + " not found.");
        }
        return preferenceRepository.findByUserAndItemTypeAndItemId(user, type, itemId)
                .map(existing -> {
                    existing.setPreferenceType(preferenceType);
                    existing.setTimestamp(LocalDateTime.now());
                    return preferenceRepository.save(existing);
                })
                .orElseGet(() -> preferenceRepository.save(
                        UserPreference.builder()
                                .user(user)
                                .itemType(type)
                                .itemId(itemId)
                                .preferenceType(preferenceType)
                                .timestamp(LocalDateTime.now())
                                .build()));
    }

    private boolean doesItemExist(UserItemType type, Long id) {
        return switch (type) {
            case RECIPE -> recipeRepository.existsById(id);
            case MEAL -> mealRepository.existsById(id);
            case FOOD_ITEM -> foodItemRepository.existsById(id);
            case MEAL_PLAN -> mealPlanRepository.existsById(id);
        };
    }

    public UserPreference togglePreference(User user, UserItemType type, Long itemId, PreferenceType newType) {
        Optional<UserPreference> existing = preferenceRepository.findByUserAndItemTypeAndItemId(user, type, itemId);

        if (existing.isPresent()) {
            PreferenceType currentType = existing.get().getPreferenceType();

            // Same button clicked again → remove
            if (currentType == newType) {
                preferenceRepository.delete(existing.get());
                return null;
            }

            // Opposite button clicked → update
            existing.get().setPreferenceType(newType);
            return preferenceRepository.save(existing.get());
        }

        // No interaction yet → create
        UserPreference pref = new UserPreference();
        pref.setUser(user);
        pref.setItemType(type);
        pref.setItemId(itemId);
        pref.setPreferenceType(newType);
        return preferenceRepository.save(pref);
    }

    public Map<String, Boolean> getPreferenceStatus(User user, UserItemType type, Long itemId) {
        Optional<UserPreference> pref = preferenceRepository.findByUserAndItemTypeAndItemId(user, type, itemId);

        boolean liked = pref.isPresent() && pref.get().getPreferenceType() == PreferenceType.LIKE;
        boolean disliked = pref.isPresent() && pref.get().getPreferenceType() == PreferenceType.DISLIKE;

        return Map.of("liked", liked, "disliked", disliked);
    }

    public Map<String, Integer> getPreferenceCounts(UserItemType type, Long itemId) {
        int likes = preferenceRepository.countByItemTypeAndItemIdAndPreferenceType(type, itemId, PreferenceType.LIKE);
        int dislikes = preferenceRepository.countByItemTypeAndItemIdAndPreferenceType(type, itemId, PreferenceType.DISLIKE);
        return Map.of("likes", likes, "dislikes", dislikes);
    }

}
