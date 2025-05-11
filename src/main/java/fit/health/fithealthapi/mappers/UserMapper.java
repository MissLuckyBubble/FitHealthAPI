package fit.health.fithealthapi.mappers;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.user.SimpleUserDTO;

public class UserMapper {
    public static SimpleUserDTO toSimpleUserDTO(User user) {
        if (user == null) return null;
        SimpleUserDTO dto = new SimpleUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

}
