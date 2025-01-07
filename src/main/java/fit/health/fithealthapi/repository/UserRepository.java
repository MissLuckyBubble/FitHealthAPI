package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u JOIN u.favoriteRecipes r WHERE r.id = :recipeId")
    int countUsersByFavoriteRecipeId(@Param("recipeId") Long recipeId);
}
