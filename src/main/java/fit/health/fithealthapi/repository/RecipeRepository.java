package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    HashSet<Recipe> findByIdIn(Set<Long> Id);
}