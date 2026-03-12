package platform.auth_service.service;

import platform.auth_service.dto.UserDTO;
import platform.auth_service.dto.UserResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserService {
    Mono<UserResponse> createUser(Map<String, String> data, Map<String, String> metadata);

    Mono<UserResponse> authenticateUser(Map<String, String> data, Map<String, String> metadata);

    Mono<UserDTO> getUser(String userId);

    Mono<UserDTO> deleteUser(String userId);

    Mono<UserDTO> updateUser(UserDTO user);
}
