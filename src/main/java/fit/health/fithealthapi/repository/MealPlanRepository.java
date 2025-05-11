package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByOwner(User user);
    @Query("""
    SELECT m FROM MealPlan m
    WHERE m.breakfast = :meal
       OR m.lunch = :meal
       OR m.dinner = :meal
       OR m.snack = :meal
""")
    List<MealPlan> findAllByMeal(@Param("meal") Meal meal);

    @Query("""
    SELECT m FROM MealPlan m
    LEFT JOIN FETCH m.breakfast
    LEFT JOIN FETCH m.lunch
    LEFT JOIN FETCH m.dinner
    LEFT JOIN FETCH m.snack
    LEFT JOIN FETCH m.macronutrients
    WHERE m.id = :id
    """)
    Optional<MealPlan> findWithMealsById(Long id);

    @Query(value = """
    SELECT
        mp.id,
        mp.name,
        mp.verified_by_admin,
        mp.visibility,
        m.id AS macronutrients_id,
        m.calories,
        m.protein,
        m.fat,
        m.sugar,
        m.salt,
        u.id AS owner_id,
        u.username,
        GROUP_CONCAT(DISTINCT dp.dietary_preferences) AS dietary_preferences,
        GROUP_CONCAT(DISTINCT a.allergens) AS allergens,
        GROUP_CONCAT(DISTINCT hcs.health_condition_suitabilities) AS health_conditions
    FROM meal_plans mp
    LEFT JOIN macronutrients m ON mp.macronutrients_id = m.id
    LEFT JOIN users u ON mp.owner_id = u.id
    LEFT JOIN meal_plan_dietary_preferences dp ON mp.id = dp.meal_plan_id
    LEFT JOIN meal_plan_allergens a ON mp.id = a.meal_plan_id
    LEFT JOIN meal_plan_health_condition_suitabilities hcs ON mp.id = hcs.meal_plan_id
    GROUP BY mp.id, m.id, u.id
""", nativeQuery = true)
    List<Object[]> findAllMealPlanRows();

}