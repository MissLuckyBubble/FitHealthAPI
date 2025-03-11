package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByUser(User user);
    List<Meal> findByVisibility(Visibility visibility);
}
