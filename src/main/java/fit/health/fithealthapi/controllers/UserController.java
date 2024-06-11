package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.OntologyService;
import fit.health.fithealthapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) {
        userService.createUser(user);
        return ResponseEntity.ok("User created successfully");
    }

    @PutMapping
    public ResponseEntity<String> editUser(@RequestBody User user, @RequestParam String oldPassword) {
        try {
            userService.editUser(user, oldPassword);
            return ResponseEntity.ok("User updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> removeUser(@PathVariable String id) {
        userService.removeUser(id);
        return ResponseEntity.ok("User removed successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestParam String username, @RequestParam String password) {
        try {
            User user = userService.loginUser(username, password);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(null);
        }
    }
}