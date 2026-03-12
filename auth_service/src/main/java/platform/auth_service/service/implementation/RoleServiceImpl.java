package platform.auth_service.service.implementation;

import platform.auth_service.repository.RolePermissionRepository;
import platform.auth_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public Mono<Boolean> hasAccess(String roleName, List<String> services,
            List<String> destinations) {
        if (services.size() != destinations.size()) {
            return Mono.just(false);
        }

        List<Tuple2<String, String>> serviceDestinations = IntStream.range(0, services.size())
                .mapToObj(i -> Tuples.of(services.get(i), destinations.get(i)))
                .toList();

        return rolePermissionRepository.findPermissionsByRoleName(roleName)
                .map(permission -> Tuples.of(permission.getService(), permission.getDestination()))
                .collect(Collectors.toSet())
                .map(permissions -> serviceDestinations.stream().allMatch(permissions::contains));
    }
}