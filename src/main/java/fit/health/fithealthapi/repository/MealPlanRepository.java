package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Set<MealPlan> findByUser(User user);
}