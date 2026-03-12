package platform.auth_service.service.implementation;

import platform.auth_service.dto.GuestUserResponse;
import platform.auth_service.repository.GuestUserRepository;
import platform.auth_service.repository.SessionRepository;
import platform.auth_service.service.GuestUserService;
import platform.auth_service.service.ReactiveJwtHandler;
import platform.auth_service.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestUserServiceImpl implements GuestUserService {

    private final GuestUserRepository guestUserRepository;
    private final SessionRepository sessionRepository;
    private final ReactiveJwtHandler jwtHandler;

    @Override
    public Mono<GuestUserResponse> createGuestUser() {
        return guestUserRepository.saveGuestUser(Roles.GUEST_USER.name())
                .flatMap(guestUserDTO ->
                        jwtHandler.createAccessToken(guestUserDTO.getUserId(), Roles.GUEST_USER.name())
                                .flatMap(accessToken ->
                                        sessionRepository.saveSession(accessToken)
                                                .map(savedSession -> {
                                                    GuestUserResponse response = new GuestUserResponse();
                                                    response.setUserId(guestUserDTO.getUserId());
                                                    response.setAccessToken(accessToken);
                                                    response.setSessionId(savedSession.getSessionId());
                                                    return response;
                                                })))
                .doOnError(e -> log.error("Error creating guest user", e))
                .onErrorMap(e -> new RuntimeException("Error creating guest user and session", e));
    }
}