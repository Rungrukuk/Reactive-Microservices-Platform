package platform.auth_service.repository;

import platform.auth_service.domain.Session;
import platform.auth_service.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    public Mono<Session> saveSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }

        String sessionId     = UUID.randomUUID().toString();
        String accessKey     = TokenHashUtil.hash(accessToken);
        String hashedSession = TokenHashUtil.hash(sessionId);
        String sessionKey    = "sid:" + hashedSession;

        return redisTemplate.opsForHash()
                .put(accessKey, "sessionId", hashedSession)
                .then(redisTemplate.expire(accessKey, SESSION_TTL))
                .then(redisTemplate.opsForValue().set(sessionKey, accessKey, SESSION_TTL))
                .thenReturn(new Session(accessToken, sessionId))
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to save session", e)));
    }

    public Mono<String> getSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }

        String accessKey = TokenHashUtil.hash(accessToken);

        return redisTemplate.opsForHash()
                .get(accessKey, "sessionId")
                .map(Object::toString)
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to retrieve session", e)));
    }

    public Mono<Boolean> validateSession(String accessToken, String sessionId) {
        if (accessToken == null || sessionId == null
                || accessToken.isEmpty() || sessionId.isEmpty()) {
            return Mono.just(false);
        }

        String accessKey     = TokenHashUtil.hash(accessToken);
        String hashedSession = TokenHashUtil.hash(sessionId);

        return redisTemplate.opsForHash()
                .get(accessKey, "sessionId")
                .map(stored -> stored.toString().equals(hashedSession))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to validate session", e)));
    }

    public Mono<Boolean> deleteByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.just(true);
        }

        String accessKey = TokenHashUtil.hash(accessToken);

        return redisTemplate.opsForHash()
                .get(accessKey, "sessionId")
                .flatMap(hashedSession -> {
                    String sessionKey = "sid:" + hashedSession.toString();
                    return redisTemplate.opsForHash()
                            .remove(accessKey, "sessionId")
                            .then(redisTemplate.delete(sessionKey))
                            .thenReturn(true);
                })
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to delete session by accessToken", e)));
    }

    public Mono<Boolean> deleteBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Mono.just(true);
        }

        String sessionKey = "sid:" + TokenHashUtil.hash(sessionId);

        return redisTemplate.opsForValue()
                .get(sessionKey)
                .flatMap(accessKey -> redisTemplate.opsForHash()
                        .remove(accessKey, "sessionId")
                        .then(redisTemplate.delete(sessionKey))
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to delete session by sessionId", e)));
    }
}