package fit.health.fithealthapi.model.dto.user;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleUserDTO {
    Long id;
    String username;
}
