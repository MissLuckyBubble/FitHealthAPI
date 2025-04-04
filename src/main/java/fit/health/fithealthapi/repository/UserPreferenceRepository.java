package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.UserPreference;
import fit.health.fithealthapi.model.enums.PreferenceType;
import fit.health.fithealthapi.model.enums.UserItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserAndItemTypeAndItemId(User user, UserItemType type, Long itemId);

    int countByItemTypeAndItemIdAndPreferenceType(UserItemType itemType, Long itemId, PreferenceType preferenceType);

    List<UserPreference> findByUserAndPreferenceTypeAndItemType(User user, PreferenceType type, UserItemType itemType);
}
