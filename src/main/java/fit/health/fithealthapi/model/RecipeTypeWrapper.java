package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fit.health.fithealthapi.model.enums.RecipeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_type_wrapper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTypeWrapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    @JsonIgnore
    private Recipe recipe;
}
