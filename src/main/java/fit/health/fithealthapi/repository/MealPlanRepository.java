package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByOwner(User user);
    List<MealPlan> findByVisibility(Visibility visibility);
    @Query("""
    SELECT m FROM MealPlan m
    WHERE m.breakfast = :meal
       OR m.lunch = :meal
       OR m.dinner = :meal
       OR m.snack = :meal
""")
    List<MealPlan> findAllByMeal(@Param("meal") Meal meal);
}