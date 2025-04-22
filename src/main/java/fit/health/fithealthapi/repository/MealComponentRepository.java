package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.MealComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

@Repository
public interface MealComponentRepository extends JpaRepository<MealComponent, Long>,
        JpaSpecificationExecutor<MealComponent> {

    @Query("SELECT mc FROM MealComponent mc LEFT JOIN FETCH mc.mealItems WHERE mc.id = :id")
    Optional<MealComponent> findByIdWithItems(@Param("id") Long id);

}
