package platform.auth_service.service.implementation;

import platform.auth_service.security.JwtTokenProvider;
import platform.auth_service.service.MonitoringClient;
import platform.auth_service.service.ReactiveJwtHandler;
import platform.auth_service.util.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReactiveJwtHandlerImpl implements ReactiveJwtHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MonitoringClient monitoringClient;
    private final Scheduler unboundedElastic;

    @Override
    public Mono<Claims> validateAccessToken(String accessToken, String userAgent,
            String clientCity, Map<String, String> metadata) {
        return validateToken(accessToken, jwtTokenProvider::getAccessTokenClaims,
                userAgent, clientCity, metadata);
    }

    @Override
    public Mono<Claims> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity, Map<String, String> metadata) {
        return validateToken(refreshToken, jwtTokenProvider::getRefreshTokenClaims,
                userAgent, clientCity, metadata);
    }

    @Override
    public Mono<Claims> validateServiceToken(Map<String, String> metadata, Destination destination) {
        return validateToken(
                metadata.getOrDefault("serviceToken", ""),
                jwtTokenProvider::getServiceTokenClaims,
                metadata.getOrDefault("userAgent", ""),
                metadata.getOrDefault("clientCity", ""),
                metadata)
                .flatMap(claims -> {
                    List<String> services = claims.get("services", List.class);
                    List<String> destinations = claims.get("destinations", List.class);
                    if (services.contains(Audience.AUTH_SERVICE.name()) &&
                            destinations.contains(destination.name())) {
                        return Mono.just(claims);
                    }
                    monitoringClient.sendEvent(
                            EventType.SERVICE_TOKEN_MISMATCH,
                            Audience.AUTH_SERVICE.name(),
                            claims.getSubject(),
                            metadata.getOrDefault("userAgent", ""),
                            metadata.getOrDefault("clientCity", ""),
                            "Valid token with incorrect permissions for destination: " + destination,
                            metadata);
                    return Mono.empty();
                });
    }

    private Mono<Claims> validateToken(String token,
            Function<String, Claims> validator,
            String userAgent,
            String clientCity,
            Map<String, String> metadata) {
        return Mono.fromCallable(() -> validator.apply(token))
                .subscribeOn(unboundedElastic)
                .onErrorResume(io.jsonwebtoken.ExpiredJwtException.class, e -> Mono.empty())
                .onErrorResume(io.jsonwebtoken.JwtException.class, e -> {
                    monitoringClient.sendEvent(
                            EventType.INVALID_JWT_FORMAT,
                            "AUTH_SERVICE",
                            null,
                            userAgent,
                            clientCity,
                            "Invalid JWT token format: " + e.getMessage(),
                            metadata);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<String> createAccessToken(String userId, String roleName) {
        return Mono.fromCallable(() -> jwtTokenProvider.createAccessToken(userId, roleName))
                .subscribeOn(unboundedElastic);
    }

    @Override
    public Mono<String> createRefreshToken(String userId, String roleName) {
        return Mono.fromCallable(() -> jwtTokenProvider.createRefreshToken(userId, roleName))
                .subscribeOn(unboundedElastic);
    }

    @Override
    public Mono<String> createServiceToken(String userId, String roleName, List<String> services,
            List<String> destinations) {
        return Mono.fromCallable(
                        () -> jwtTokenProvider.createServiceToken(userId, roleName, services, destinations))
                .subscribeOn(unboundedElastic);
    }

    @Override
    public Mono<Claims> parseAccessTokenClaims(String accessToken) {
        return Mono.fromCallable(() -> jwtTokenProvider.getAccessTokenClaims(accessToken))
                .subscribeOn(unboundedElastic);
    }

    @Override
    public Mono<Claims> parseRefreshTokenClaims(String refreshToken) {
        return Mono.fromCallable(() -> jwtTokenProvider.getRefreshTokenClaims(refreshToken))
                .subscribeOn(unboundedElastic);
    }

    @Override
    public Mono<Claims> parseServiceTokenClaims(String serviceToken) {
        return Mono.fromCallable(() -> jwtTokenProvider.getServiceTokenClaims(serviceToken))
                .subscribeOn(unboundedElastic);
    }
}