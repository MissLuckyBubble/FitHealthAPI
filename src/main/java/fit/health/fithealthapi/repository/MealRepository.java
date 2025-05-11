package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.enums.Visibility;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByOwner(User user);
    List<Meal> findByVisibility(Visibility visibility);

    @Query("""
    SELECT m FROM Meal m
    LEFT JOIN FETCH m.macronutrients
    LEFT JOIN FETCH m.mealItems mi
    LEFT JOIN FETCH mi.macronutrients
    WHERE m.id = :id
    """)
    Optional<Meal> findByIdFullyLoaded(@Param("id") Long id);

    @Query(
            value = "SELECT DISTINCT m.* FROM meals m " +
                    "LEFT JOIN meal_items mi ON mi.meal_id = m.id " +
                    "LEFT JOIN meal_component mc ON mi.component_id = mc.id " +
                    "LEFT JOIN recipes r ON mc.id = r.id " +
                    "LEFT JOIN recipe_type_wrapper rw ON rw.recipe_id = r.id " +
                    "LEFT JOIN recipe_ingredients ri ON ri.recipe_id = r.id " +
                    "WHERE m.id IN (:ids)",
            nativeQuery = true
    )
    List<Meal> fetchWithAllDependencies(@Param("ids") List<Long> ids);
}
