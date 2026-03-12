package platform.auth_service.repository;

import platform.auth_service.domain.Role;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RoleRepository extends ReactiveCrudRepository<Role, String> {
}
