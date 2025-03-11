package fit.health.fithealthapi.model.dto;

import fit.health.fithealthapi.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
public class EditUserDTO {
    private User user;
    private String oldPassword;
}
