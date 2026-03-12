package platform.auth_service.service;

import platform.auth_service.dto.GuestUserResponse;
import reactor.core.publisher.Mono;

public interface GuestUserService {
    public Mono<GuestUserResponse> createGuestUser();
}
