package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private User user;
}
