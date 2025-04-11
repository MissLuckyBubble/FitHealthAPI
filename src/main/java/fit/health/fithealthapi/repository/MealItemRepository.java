package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.MealComponent;
import fit.health.fithealthapi.model.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealItemRepository extends JpaRepository<MealItem, Long> {
    List<MealItem> findByComponent(MealComponent component);
}
