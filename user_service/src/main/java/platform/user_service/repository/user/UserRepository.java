package platform.user_service.repository.user;

import platform.user_service.domain.user.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository extends ReactiveCrudRepository<User, String> {
}
