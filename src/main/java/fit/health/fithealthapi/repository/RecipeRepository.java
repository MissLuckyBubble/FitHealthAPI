package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    HashSet<Recipe> findByIdIn(Set<Long> Id);
    @Query("SELECT r FROM Recipe r JOIN r.ingredients i WHERE i.foodItem = :foodItem")
    List<Recipe> findAllByIngredient(@Param("foodItem") FoodItem foodItem);
}