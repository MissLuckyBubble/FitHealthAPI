package fit.health.fithealthapi.model;

import fit.health.fithealthapi.model.enums.PreferenceType;
import fit.health.fithealthapi.model.enums.UserItemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_preferences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "itemId", "itemType"}))
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private PreferenceType preferenceType; // LIKE / DISLIKE

    @Enumerated(EnumType.STRING)
    private UserItemType itemType; // RECIPE, MEAL, FOOD_ITEM

    private Long itemId;

    private LocalDateTime timestamp = LocalDateTime.now();
}
