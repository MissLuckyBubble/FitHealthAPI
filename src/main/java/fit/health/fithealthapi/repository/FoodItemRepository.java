package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    Optional<FoodItem> findByName(String name);
}