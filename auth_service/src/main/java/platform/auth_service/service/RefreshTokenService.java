package platform.auth_service.service;

import platform.auth_service.domain.RefreshToken;
import reactor.core.publisher.Mono;

public interface RefreshTokenService {

    public Mono<Boolean> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity);

    public Mono<Boolean> deleteByRefreshToken(String refreshToken);

    public Mono<RefreshToken> createOrUpdateRefreshTokenEntity(String userId, String refreshToken,
            String userAgent,
            String clientCity);

    public Mono<RefreshToken> createRefreshTokenEntity(String userId, String refreshToken,
            String userAgent,
            String clientCity);

    public Mono<RefreshToken> updateRefreshTokenEntity(String userId, String refreshToken,
            String userAgent,
            String clientCity);
}
