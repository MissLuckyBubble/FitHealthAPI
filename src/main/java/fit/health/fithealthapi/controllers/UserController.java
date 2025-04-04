package fit.health.fithealthapi.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.exceptions.UserNotFoundException;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.EditUserDTO;
import fit.health.fithealthapi.model.dto.LoginUserDTO;
import fit.health.fithealthapi.model.enums.Role;
import fit.health.fithealthapi.services.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping()
    public ResponseEntity<?> registerUser(@RequestBody LoginUserDTO userDTO) {
        try {
            User user = userService.saveUser(userDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody EditUserDTO userDto, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);

            if (!hasPermissionToEdit(currentUser, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to edit this user.");
            }

            if (isSelfUpdateWithValidCredentials(currentUser, userDto.getOldPassword()) || isAdmin(currentUser)) {
                User updatedUser = userService.updateUser(id, userDto.getUser());
                return ResponseEntity.ok(updatedUser);
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid credentials.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean hasPermissionToEdit(User currentUser, Long targetUserId) {
        return currentUser.getRole().equals(Role.ADMIN) || currentUser.getId().equals(targetUserId);
    }

    // Helper method to validate self-update credentials
    private boolean isSelfUpdateWithValidCredentials(User currentUser, String oldPassword) {
        return userService.checkCredentials(currentUser.getUsername(), oldPassword);
    }

    // Helper method to check if the user is an admin
    private boolean isAdmin(User currentUser) {
        return currentUser.getRole().equals(Role.ADMIN);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this user.");
            }
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }


    @GetMapping()
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "range", required = false) String range,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        try {

            // Parse filters
            Map<String, String> filters = filter != null
                    ? new ObjectMapper().readValue(filter, new TypeReference<>() {
            })
                    : Collections.emptyMap();

            // Parse range
            int start = 0, end = 10;
            if (range != null) {
                int[] rangeArray = new ObjectMapper().readValue(range, int[].class);
                start = rangeArray[0];
                end = rangeArray[1] + 1;
            }

            // Parse sort
            String sortField = "id";
            String sortOrder = "ASC";
            if (sort != null) {
                String[] sortArray = new ObjectMapper().readValue(sort, String[].class);
                sortField = sortArray[0];
                sortOrder = sortArray[1];
            }

            // Fetch users with filters, sorting, and pagination
            List<User> users = userService.getAllWithFilters(filters, sortField, sortOrder, start, end);

            // Total count for pagination
            long total = userService.getTotalCount(filters);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "users " + start + "-" + (end - 1) + "/" + total);

            return ResponseEntity.ok().headers(headers).body(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        }catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}
