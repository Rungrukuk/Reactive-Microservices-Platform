package platform.auth_service.service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Claims;

public interface ReactiveJwtHandler {
    public Mono<Claims> validateAccessToken(String accessToken, String userAgent,
            String clientCity,
            Map<String, String> metadata);

    public Mono<Claims> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity,
            Map<String, String> metadata);

    public Mono<Claims> validateServiceToken(Map<String, String> metadata, Destination destination);

    public Mono<String> createAccessToken(String userId, String roleName);

    public Mono<String> createRefreshToken(String userId, String roleName);
   
    public Mono<String> createServiceToken(String userId, String roleName, List<String> services, List<String> destinations);

    public Mono<Claims> parseAccessTokenClaims(String accessToken);
    
    public Mono<Claims> parseRefreshTokenClaims(String refreshToken);

    public Mono<Claims> parseServiceTokenClaims(String serviceToken);

    public enum Audience {
        AUTH_SERVICE
    }


    public enum Destination {
        REGISTER,
        LOGIN
    }
}
