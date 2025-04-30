package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    private PreferenceType preferenceType; // LIKE / DISLIKE

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private UserItemType itemType;
// RECIPE, MEAL, FOOD_ITEM, MEAL_PLAN

    private Long itemId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

}
