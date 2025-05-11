package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.DiaryEntry;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    List<DiaryEntry> findByOwner(User user);
    @EntityGraph(attributePaths = {
            "macronutrients",
            "dietaryPreferences",
            "allergens",
            "healthConditionSuitabilities",
            "owner",
            "breakfast",
            "breakfast.macronutrients",
            "breakfast.dietaryPreferences",
            "breakfast.allergens",
            "breakfast.healthConditionSuitabilities",
            "breakfast.mealItems",
            "breakfast.mealItems.macronutrients",
            "lunch",
            "lunch.macronutrients",
            "lunch.dietaryPreferences",
            "lunch.allergens",
            "lunch.healthConditionSuitabilities",
            "lunch.mealItems",
            "lunch.mealItems.macronutrients",
            "dinner",
            "dinner.macronutrients",
            "dinner.dietaryPreferences",
            "dinner.allergens",
            "dinner.healthConditionSuitabilities",
            "dinner.mealItems",
            "dinner.mealItems.macronutrients",
            "snack",
            "snack.macronutrients",
            "snack.dietaryPreferences",
            "snack.allergens",
            "snack.healthConditionSuitabilities",
            "snack.mealItems",
            "snack.mealItems.macronutrients",
            "breakfast.mealItems.component",
            "lunch.mealItems.component",
            "dinner.mealItems.component",
            "snack.mealItems.component"
    })
    @Query("SELECT d FROM DiaryEntry d WHERE d.owner.id = :userId AND d.date = :date")
    Optional<DiaryEntry> findByOwnerIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    @Query("""
    SELECT d FROM DiaryEntry d
    WHERE d.breakfast = :meal
       OR d.lunch = :meal
       OR d.dinner = :meal
       OR d.snack = :meal
""")
    List<DiaryEntry> findByAnyMeal(@Param("meal") Meal meal);

    @Query("SELECT d FROM DiaryEntry d WHERE d.date >= :cutoffDate AND d.visibility = 'PRIVATE' AND d.owner.id = :ownerId")
    List<DiaryEntry> findRecentEntriesByOwnerId(@Param("ownerId") Long ownerId,
                                               @Param("cutoffDate") LocalDate cutoffDate);

}
