package platform.auth_service.util;

import platform.auth_service.domain.User;
import platform.auth_service.dto.UserDTO;

public class UserMapper {

    public static UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setEmail(user.getEmail());
        userDTO.setRoleName(user.getRoleName());

        return userDTO;
    }

    public static User toUserEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setUserId(userDTO.getUserId());
        user.setEmail(userDTO.getEmail());
        user.setRoleName(userDTO.getRoleName());

        return user;
    }
}
