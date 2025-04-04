package fit.health.fithealthapi.model;

import fit.health.fithealthapi.interfeces.NutritionalSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MealComponent extends NutritionalProfile implements NutritionalSource {
    protected String name;
    protected String ontologyLinkedName;
}
