package platform.user_service.service;

import platform.user_service.dto.UserResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserService {
    Mono<UserResponse> createUserDetails(Map<String, String> data);

    Mono<UserResponse> getUserDetails(String userId);

    Mono<UserResponse> deleteUserDetails(String userId);

    Mono<UserResponse> updateUserDetails(UserResponse user);
}
