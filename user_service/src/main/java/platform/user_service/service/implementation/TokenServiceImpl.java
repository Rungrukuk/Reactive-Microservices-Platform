package platform.user_service.service.implementation;

import platform.user_service.service.MonitoringClient;
import platform.user_service.service.TokenService;
import platform.user_service.util.EventType;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final PublicKey serviceTokenPublicKey;
    private final JwtParser jwtParser;
    private final MonitoringClient monitoringClient;

    public TokenServiceImpl(
            @Value("${jwt.service.public.key}") String servicePublicKeyStr,
            MonitoringClient monitoringClient)
            throws Exception {

        this.serviceTokenPublicKey = getPublicKeyFromString(servicePublicKeyStr);

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(serviceTokenPublicKey)
                .setAllowedClockSkewSeconds(60)
                .build();
        this.monitoringClient = monitoringClient;
    }

    private PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    @Override
    public Mono<String> validateTokenAndGetUserId(
            Map<String, String> metadata,
            Destination destination) {

        String token = metadata.get("serviceToken");
        if (token == null || token.isEmpty()) {
            monitoringClient.sendEvent(
                    EventType.SERVICE_TOKEN_MISMATCH,
                    Audience.USER_SERVICE.name(),
                    "",
                    metadata.getOrDefault("userAgent", ""),
                    metadata.getOrDefault("clientCity", ""),
                    "Missing service token for destination: " + destination,
                    metadata);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> jwtParser.parseClaimsJws(token).getBody())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(claims -> {
                    List<String> services = claims.get("services", List.class);
                    List<String> destinations = claims.get("destinations", List.class);

                    if (services.contains(Audience.USER_SERVICE.name()) &&
                            destinations.contains(destination.name())) {
                        return Mono.just(claims.getSubject());
                    }
                    monitoringClient.sendEvent(
                            EventType.SERVICE_TOKEN_MISMATCH,
                            Audience.USER_SERVICE.name(),
                            claims.getSubject(),
                            metadata.getOrDefault("userAgent", ""),
                            metadata.getOrDefault("clientCity", ""),
                            "Valid token with incorrect permissions for destination: " + destination,
                            metadata);

                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    monitoringClient.sendEvent(
                            EventType.SERVICE_TOKEN_MISMATCH,
                            Audience.USER_SERVICE.name(),
                            "",
                            metadata.getOrDefault("userAgent", ""),
                            metadata.getOrDefault("clientCity", ""),
                            "Invalid service token for destination: " + destination,
                            metadata);
                    return Mono.empty();
                });
    }

}
