package platform.api_gateway.security;

import platform.api_gateway.ProtoAuthRequest;
import platform.api_gateway.ProtoAuthResponse;
import platform.api_gateway.service.RSocketService;
import platform.api_gateway.util.AuthResponseStatuses;
import platform.api_gateway.util.Constants;
import platform.api_gateway.util.ServiceDestination;
import platform.api_gateway.util.ServiceDestinationMapper;
import platform.api_gateway.util.Services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenValidationFilter implements WebFilter {

    private final RSocketService rSocketService;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange,
            @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        ServiceDestination serviceDestination = ServiceDestinationMapper.getMapping(path);

        List<String> services = serviceDestination.getServices();
        List<String> destinations = serviceDestination.getDestinations();

        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

        String accessToken = Optional.ofNullable(cookies.getFirst("accessToken"))
                .map(HttpCookie::getValue)
                .orElse("");

        String sessionId = Optional.ofNullable(cookies.getFirst("sessionId"))
                .map(HttpCookie::getValue)
                .orElse("");

        String refreshToken = Optional.ofNullable(cookies.getFirst("refreshToken"))
                .map(HttpCookie::getValue)
                .orElse("");

        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");

        return validateToken(accessToken, sessionId, refreshToken, services, destinations,
                userAgent,
                clientCity)
                .flatMap(
                        authResponse -> handleAuthResponse(exchange, chain, authResponse, userAgent,
                                clientCity))
                .onErrorResume(e -> {
                    log.error(e.getMessage());
                    return handleErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                            "Unexpected error");
                });
    }

    private Mono<Void> handleAuthResponse(ServerWebExchange exchange, WebFilterChain chain,
            ProtoAuthResponse authResponse, String userAgent, String clientCity) {
        String status = authResponse.getStatus();
        AuthResponseStatuses authStatus = AuthResponseStatuses.valueOf(status);
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer dataBuffer = dataBufferFactory
                .wrap(getJsonResponse(status).getBytes(StandardCharsets.UTF_8));

        switch (authStatus) {
            case AUTHORIZED_USER:
            case AUTHORIZED_GUEST_USER:
                CustomAuthentication authentication = new CustomAuthentication(
                        authResponse.getMetadataMap(), status);
                assignCookies(exchange, authResponse);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder
                                .withAuthentication(authentication));

            case UNAUTHORIZED_USER:
            case UNAUTHORIZED_GUEST_USER:
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                assignCookies(exchange, authResponse);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(dataBuffer));

            case UNAUTHENTICATED_GUEST_USER:
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                assignCookies(exchange, authResponse);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(dataBuffer));

            case UNEXPECTED_ERROR:
            default:
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        }
    }

    private Mono<Void> handleErrorResponse(ServerWebExchange exchange, HttpStatus status,
            String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory()
                .wrap("{\"error\":\"%s\"}".formatted(message).getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(dataBuffer))
                .doFinally(signalType -> log.info(status.toString()));
    }

    private String getJsonResponse(String status) {
        return "{\"userStatus\":\"%s\"}".formatted(status);
    }

    private void assignCookies(ServerWebExchange exchange, ProtoAuthResponse authResponse) {
        String newAccessToken = authResponse.getMetadataOrDefault("accessToken", "");
        String newSessionId = authResponse.getMetadataOrDefault("sessionId", "");
        String newRefreshToken = authResponse.getMetadataOrDefault("refreshToken", "");
        exchange.getResponse().addCookie(ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(Constants.ACCESS_TOKEN_COOKIE_LIFETIME)
                .build());

        exchange.getResponse().addCookie(ResponseCookie.from("sessionId", newSessionId)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(Constants.SESSION_ID_LIFETIME)
                .build());

        exchange.getResponse().addCookie(ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(Constants.REFRESH_TOKEN_COOKIE_LIFETIME)
                .build());
    }

    private Mono<ProtoAuthResponse> validateToken(String accessToken, String sessionId,
            String refreshToken,
            List<String> services, List<String> destinations, String userAgent, String clientCity) {
        ProtoAuthRequest protoAuthRequest = ProtoAuthRequest.newBuilder()
                    .setAccessToken(accessToken)
                    .setSessionId(sessionId)
                    .setRefreshToken(refreshToken)
                    .addAllServices(services)
                    .addAllDestinations(destinations)
                .setUserAgent(userAgent)
                .setClientCity(clientCity)
                .build();

        return rSocketService.getRSocketRequester(Services.AUTH_SERVICE)
                            .route("auth.validateToken")
                            .data(protoAuthRequest)
                .retrieveMono(ProtoAuthResponse.class)
                .doOnError(e -> {
                    log.error("Error processing authentication request", e);
                });
    }
}
