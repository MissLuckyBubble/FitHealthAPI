package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.LoginUserDTO;
import fit.health.fithealthapi.model.enums.*;
import fit.health.fithealthapi.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public UserService(UserRepository userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public User saveUser(LoginUserDTO user) {
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
        existingUser.setGender(updatedUser.getGender());
        existingUser.setDietaryPreferences(updatedUser.getDietaryPreferences());
        existingUser.setHealthConditions(updatedUser.getHealthConditions());
        existingUser.setAllergens(updatedUser.getAllergens());
        existingUser.setActivityLevel(updatedUser.getActivityLevel());
        existingUser.setGoal(updatedUser.getGoal());

        boolean hasValidData = updatedUser.getWeightKG() > 0 &&
                updatedUser.getHeightCM() > 0 &&
                updatedUser.getBirthDate() != null &&
                !updatedUser.getBirthDate().isEmpty();

        if(updatedUser.getDailyCalorieGoal() == 0){
            if (hasValidData) {
                double bmr = calculateBMR(
                        updatedUser.getWeightKG(),
                        updatedUser.getHeightCM(),
                        calculateAge(updatedUser.getBirthDate()),
                        updatedUser.getGender()
                );
                double tdee = calculateTDEE(bmr, updatedUser.getActivityLevel());
                double newCalorieGoal = calculateCaloriesForGoal(tdee, updatedUser.getGoal());
                existingUser.setDailyCalorieGoal((float) newCalorieGoal);
            }else{
                existingUser.setDailyCalorieGoal(existingUser.getDailyCalorieGoal());
            }
        } else {
            existingUser.setDailyCalorieGoal(updatedUser.getDailyCalorieGoal());
        }

        return userRepository.save(existingUser);
    }

    public double calculateTDEE(double bmr, ActivityLevel activityLevel) {
        return bmr * activityLevel.getMultiplier();
    }

    public double calculateBMR(double weightKG, double heightCM, int age, Gender gender) {
        if (gender == Gender.MALE) {
            return (10 * weightKG) + (6.25 * heightCM) - (5 * age) + 5;
        } else {
            return (10 * weightKG) + (6.25 * heightCM) - (5 * age) - 161;
        }
    }

    public double calculateCaloriesForGoal(double tdee, Goal goal) {
        return tdee + goal.getCalorieAdjustment();
    }
    public int calculateAge(String birthDate) {
        LocalDate birth = LocalDate.parse(birthDate);
        return Period.between(birth, LocalDate.now()).getYears();
    }

    public boolean checkCredentials(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if(optionalUser.isEmpty()) {
           return false;
        }
        User user = optionalUser.get();
        return verifyPassword(password, user.getPassword());
    }

    private String hashPassword(String password) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawPassword, hashedPassword);
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

    private Predicate buildUserPredicate(CriteriaBuilder cb, Root<User> user, Map<String, String> filters) {
        Predicate predicate = cb.conjunction();

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            predicate = cb.and(predicate, cb.equal(user.get(filter.getKey()), filter.getValue()));
        }

        return predicate;
    }

    public List<User> getAllWithFilters(Map<String, String> filters, String sortField, String sortOrder, int start, int end) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        Predicate predicate = buildUserPredicate(cb, user, filters);
        query.where(predicate);

        if ("ASC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.asc(user.get(sortField)));
        } else if ("DESC".equalsIgnoreCase(sortOrder)) {
            query.orderBy(cb.desc(user.get(sortField)));
        }

        TypedQuery<User> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(start);
        typedQuery.setMaxResults(end - start + 1);

        return typedQuery.getResultList();
    }

    public long getTotalCount(Map<String, String> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> user = query.from(User.class);

        Predicate predicate = buildUserPredicate(cb, user, filters);
        query.where(predicate);

        query.select(cb.count(user));
        return entityManager.createQuery(query).getSingleResult();
    }
}

