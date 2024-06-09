package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.services.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private OntologyService ontologyService;

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) {
        ontologyService.createUser(user);
        return ResponseEntity.ok("User created successfully");
    }

    @PutMapping
    public ResponseEntity<String> editUser(@RequestBody User user, @RequestParam String oldPassword) {
        try {
            ontologyService.editUser(user, oldPassword);
            return ResponseEntity.ok("User updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> removeUser(@PathVariable String id) {
        ontologyService.removeUser(id);
        return ResponseEntity.ok("User removed successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestParam String username, @RequestParam String password) {
        try {
            User user = ontologyService.loginUser(username, password);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(null);
        }
    }
}