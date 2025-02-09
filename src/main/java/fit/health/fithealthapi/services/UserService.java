package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.UserDTO;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    public User saveUser(UserDTO user) {
        Optional<User> userOptional = userRepository.findByUsername(user.getUsername());
        if (userOptional.isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        // Hash the password before saving
        User createdUser = new User();
        createdUser.setPassword(hashPassword(user.getPassword()));
        createdUser.setUsername(user.getUsername());
        createdUser.setRole(Role.USER);
        return userRepository.save(createdUser);
    }

    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        existingUser.setUsername(updatedUser.getUsername());
        if(updatedUser.getPassword() != null){
            existingUser.setPassword(hashPassword(updatedUser.getPassword()));
        }
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setBirthDate(updatedUser.getBirthDate());
        existingUser.setWeightKG(updatedUser.getWeightKG());
        existingUser.setGoalWeight(updatedUser.getGoalWeight());
        existingUser.setHeightCM(updatedUser.getHeightCM());
        existingUser.setDailyCalorieGoal(updatedUser.getDailyCalorieGoal());
        existingUser.setGender(updatedUser.getGender());
        existingUser.setDietaryPreferences(updatedUser.getDietaryPreferences());
        existingUser.setHealthConditions(updatedUser.getHealthConditions());
        existingUser.setAllergens(updatedUser.getAllergens());

        return userRepository.save(existingUser);
    }

    public boolean checkCredentials(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if(optionalUser.isEmpty()) {
           return false;
        }
        User user = optionalUser.get();
        if (!verifyPassword(password, user.getPassword())) {
           return false;
        }
        return true;
    }

    private String hashPassword(String password) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawPassword, hashedPassword);
    }

    public boolean addFavoriteRecipe(Long userId, Recipe recipe) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if(user.getFavoriteRecipes().add(recipe)){
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean removeFavoriteRecipe(Long userId, Recipe recipe) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if(user.getFavoriteRecipes().remove(recipe)){
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void deleteUser(Long id) {
        userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        userRepository.deleteById(id);
    }

    public User getUserById(Long userId){
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User getUserByUsername(String currentUsername) {
        return userRepository.findByUsername(currentUsername).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public List<Recipe> getFavoriteRecipes(Long userId) {
        return userRepository.findFavoriteRecipesByUserId(userId);
    }

    public List<User> getAllWithFilters(Map<String, String> filters, String sortField, String sortOrder, int start, int end) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        // Add dynamic filters
        Predicate predicate = cb.conjunction();
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            predicate = cb.and(predicate, cb.equal(user.get(filter.getKey()), filter.getValue()));
        }
        query.where(predicate);

        // Add sorting
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.asc(user.get(sortField)));
        } else if ("DESC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.desc(user.get(sortField)));
        }

        // Execute the query with pagination
        TypedQuery<User> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(start);
        typedQuery.setMaxResults(end - start + 1);

        return typedQuery.getResultList();
    }

    public long getTotalCount(Map<String, String> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> user = query.from(User.class);

        // Add dynamic filters
        Predicate predicate = cb.conjunction();
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            predicate = cb.and(predicate, cb.equal(user.get(filter.getKey()), filter.getValue()));
        }
        query.where(predicate);

        // Set count query
        query.select(cb.count(user));

        return entityManager.createQuery(query).getSingleResult();
    }

}

