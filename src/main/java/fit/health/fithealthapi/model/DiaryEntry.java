package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "diary_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;  // Entry for a specific day

    @Column(nullable = false)
    private Float dailyCalorieGoal;

    @OneToOne
    @JoinColumn(name = "breakfast_id")
    private Meal breakfast;

    @OneToOne
    @JoinColumn(name = "lunch_id")
    private Meal lunch;

    @OneToOne
    @JoinColumn(name = "dinner_id")
    private Meal dinner;

    @OneToOne
    @JoinColumn(name = "snack_id")
    private Meal snack;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "macronutrients_id", nullable = false)
    private Macronutrients macronutrients;
}
