package platform.auth_service.service;

import platform.auth_service.ProtoAuthRequest;
import platform.auth_service.dto.AuthResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
    public Mono<AuthResponse> validate(ProtoAuthRequest metadata);
}
