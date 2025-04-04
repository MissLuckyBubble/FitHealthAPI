package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.InvalidRequestException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.UserPreference;
import fit.health.fithealthapi.model.enums.PreferenceType;
import fit.health.fithealthapi.model.enums.UserItemType;
import fit.health.fithealthapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository repository;
    private final RecipeRepository recipeRepository;
    private final MealRepository mealRepository;
    private final FoodItemRepository foodItemRepository;

    public UserPreference like(User user, UserItemType type, Long itemId) {
        return saveOrUpdate(user, type, itemId, PreferenceType.LIKE);
    }

    public UserPreference dislike(User user, UserItemType type, Long itemId) {
        return saveOrUpdate(user, type, itemId, PreferenceType.DISLIKE);
    }

    public List<UserPreference> getLikedItemsByType(User user, UserItemType type) {
        return repository.findByUserAndPreferenceTypeAndItemType(user, PreferenceType.LIKE, type);
    }

    public List<UserPreference> getDislikedItemsByType(User user, UserItemType type) {
        return repository.findByUserAndPreferenceTypeAndItemType(user, PreferenceType.DISLIKE, type);
    }

    private UserPreference saveOrUpdate(User user, UserItemType type, Long itemId, PreferenceType preferenceType) {
        if (user == null || type == null || itemId == null) {
            throw new InvalidRequestException("User, item type, and item ID must be provided.");
        }
        if (!doesItemExist(type, itemId)) {
            throw new NotFoundException("Item of type " + type + " with ID " + itemId + " not found.");
        }
        return repository.findByUserAndItemTypeAndItemId(user, type, itemId)
                .map(existing -> {
                    existing.setPreferenceType(preferenceType);
                    existing.setTimestamp(LocalDateTime.now());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(
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
        };
    }

}
