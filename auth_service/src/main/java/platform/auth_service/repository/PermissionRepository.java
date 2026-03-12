package platform.auth_service.repository;

import platform.auth_service.domain.Permission;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PermissionRepository extends ReactiveCrudRepository<Permission, Long> {
}
