package fit.health.fithealthapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "macronutrients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Macronutrients {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float calories = 0f;

    @Column(nullable = false)
    private Float protein = 0f;

    @Column(nullable = false)
    private Float fat = 0f;

    @Column(nullable = false)
    private Float sugar = 0f;

    @Column(nullable = false)
    private Float salt = 0f;

    public void add(Macronutrients other) {
        this.calories += other.calories;
        this.protein += other.protein;
        this.fat += other.fat;
        this.sugar += other.sugar;
        this.salt += other.salt;
    }

    public void reset() {
        this.calories = 0f;
        this.protein = 0f;
        this.fat = 0f;
        this.sugar = 0f;
        this.salt = 0f;
    }
}

