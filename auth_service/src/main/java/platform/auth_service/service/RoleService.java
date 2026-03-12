package platform.auth_service.service;

import reactor.core.publisher.Mono;

import java.util.List;

public interface RoleService {
    public Mono<Boolean> hasAccess(String roleName, List<String> services,
            List<String> destinations);
}
