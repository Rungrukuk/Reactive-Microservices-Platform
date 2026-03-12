package platform.auth_service.repository;

import platform.auth_service.domain.User;
import platform.auth_service.dto.UserDTO;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String> {

    Mono<UserDTO> findUserDtoByUserId(String userId);

    Mono<UserDTO> findUserDtoByEmail(String email);

    Mono<User> findUserByEmail(String email);

}
