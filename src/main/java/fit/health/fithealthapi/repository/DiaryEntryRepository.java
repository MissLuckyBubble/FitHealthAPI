package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.DiaryEntry;
import fit.health.fithealthapi.model.Meal;
import fit.health.fithealthapi.model.User;
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
}
