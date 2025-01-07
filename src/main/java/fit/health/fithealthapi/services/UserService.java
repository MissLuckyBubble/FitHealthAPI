package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.EditUserDTO;
import fit.health.fithealthapi.model.dto.UserDTO;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SharedService sharedService;

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

    public User updateUser(Long id, EditUserDTO updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        checkCredentials(existingUser.getUsername(),updatedUser.getOldPassword());

        if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                userRepository.findByUsername(updatedUser.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setPassword(hashPassword(updatedUser.getPassword()));
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setBirthDate(updatedUser.getBirthDate());
        existingUser.setWeightKG(updatedUser.getWeightKG());
        existingUser.setGoalWeight(updatedUser.getGoalWeight());
        existingUser.setHeightCM(updatedUser.getHeightCM());
        existingUser.setDailyCalorieGoal(updatedUser.getDailyCalorieGoal());
        existingUser.setGender(sharedService.convertToGender(updatedUser.getGender()));
        existingUser.setDietaryPreferences(
                new HashSet<>(sharedService.convertToDietaryPreferences(updatedUser.getDietaryPreferences()))
        );
        existingUser.setHealthConditions(
                new HashSet<>(sharedService.convertToHealthCondition(updatedUser.getHealthConditions()))
        );

        return userRepository.save(existingUser);
    }

    public User checkCredentials(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if(optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        User user = optionalUser.get();
        if (!verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return user;
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

    public Optional<User> getUserById(Long userId){
        return userRepository.findById(userId);
    }

    public User getUserByUsername(String currentUsername) {
        return userRepository.findByUsername(currentUsername).orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}

