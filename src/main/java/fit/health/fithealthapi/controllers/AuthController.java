package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.JwtResponse;
import fit.health.fithealthapi.model.dto.LoginUserDTO;
import fit.health.fithealthapi.repository.UserRepository;
import fit.health.fithealthapi.utils.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    @Lazy
    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginUserDTO userDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword())
            );

            String token = jwtUtil.generateToken(userDTO.getUsername());
            Optional<User> optionalUser = userRepository.findByUsername(userDTO.getUsername());
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                return ResponseEntity.ok(new JwtResponse(token,user));
            }else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

}