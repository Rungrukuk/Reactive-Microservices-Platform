package platform.api_gateway.controller;

import platform.api_gateway.ProtoRequest;
import platform.api_gateway.ProtoResponse;
import platform.api_gateway.security.CustomAuthentication;
import platform.api_gateway.service.RSocketService;
import platform.api_gateway.util.AuthResponseStatuses;
import platform.api_gateway.util.Constants;
import platform.api_gateway.util.Services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
@RequestMapping("/")
@RequiredArgsConstructor
public class ApiGatewayController {

    private final RSocketService rSocketService;

    @GetMapping
    public Mono<ResponseEntity<Map<String, String>>> getRoot() {
        return ReactiveSecurityContextHolder.getContext().flatMap(
                securityContext -> {
                    CustomAuthentication authentication = (CustomAuthentication) securityContext
                            .getAuthentication();
                    return Mono.just(new ResponseEntity<>(
                            Map.of("userStatus", authentication.getUserStatus()),
                            HttpStatus.OK));
                });
    }

    @PostMapping("register")
    public Mono<ResponseEntity<Map<String, String>>> registerUser(
            @RequestBody Map<String, String> body,
            ServerWebExchange exchange) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    CustomAuthentication authentication = (CustomAuthentication) securityContext
                            .getAuthentication();
                    String serviceToken = authentication.getPrincipal().get("serviceToken");
                    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
                    String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");

                    ProtoRequest protoRequest = ProtoRequest.newBuilder()
                            .putMetadata("serviceToken", serviceToken)
                            .putMetadata("userAgent", userAgent)
                            .putMetadata("clientCity", clientCity)
                                    .putData("email", body.getOrDefault("email", ""))
                                    .putData("password", body.getOrDefault("password", ""))
                                    .putData("rePassword", body.getOrDefault("rePassword", ""))
                            .build();

                    return rSocketService.getRSocketRequester(Services.AUTH_SERVICE)
                                            .route("auth.registerUser")
                                            .data(protoRequest)
                            .retrieveMono(ProtoResponse.class)
                            .flatMap(protoResponse -> {
                                Map<String, String> responseBody = createResponseBody(
                                        authentication, protoResponse);
                                setCookiesIfPresent(exchange, protoResponse);
                                HttpStatus status = HttpStatus.valueOf(protoResponse.getStatusCode());
                                return Mono.just(new ResponseEntity<>(responseBody, status));
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing register request", e);
                                return Mono.just(new ResponseEntity<>(
                                        Map.of("error", "Registration failed"),
                                        HttpStatus.INTERNAL_SERVER_ERROR));
                            });
                });
    }

    @PostMapping("login")
    public Mono<ResponseEntity<Map<String, String>>> loginUser(
            @RequestBody Map<String, String> body,
            ServerWebExchange exchange) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    CustomAuthentication authentication = (CustomAuthentication) securityContext
                            .getAuthentication();
                    String serviceToken = authentication.getPrincipal().get("serviceToken");
                    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
                    String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");

                    ProtoRequest protoRequest = ProtoRequest.newBuilder()
                            .putMetadata("serviceToken", serviceToken)
                            .putMetadata("userAgent", userAgent)
                            .putMetadata("clientCity", clientCity)
                                    .putData("email", body.getOrDefault("email", ""))
                                    .putData("password", body.getOrDefault("password", ""))
                            .build();

                    return rSocketService.getRSocketRequester(Services.AUTH_SERVICE)
                                            .route("auth.loginUser")
                                            .data(protoRequest)
                            .retrieveMono(ProtoResponse.class)
                            .flatMap(protoResponse -> {
                                Map<String, String> responseBody = createResponseBody(
                                        authentication, protoResponse);
                                setCookiesIfPresent(exchange, protoResponse);
                                HttpStatus status = HttpStatus.valueOf(protoResponse.getStatusCode());
                                return Mono.just(new ResponseEntity<>(responseBody, status));
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing login request", e);
                                return Mono.just(new ResponseEntity<>(
                                        Map.of("error", "Login failed"),
                                        HttpStatus.INTERNAL_SERVER_ERROR));
                            });
                });
    }

    @PostMapping("user-details")
    public Mono<ResponseEntity<Map<String, String>>> createUserDetails(
            @RequestBody Map<String, String> body,
            ServerWebExchange exchange) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    CustomAuthentication authentication = (CustomAuthentication) securityContext
                            .getAuthentication();
                    String serviceToken = authentication.getPrincipal().get("serviceToken");
                    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
                    String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");
                    ProtoRequest protoRequest = ProtoRequest.newBuilder()
                            .putMetadata("serviceToken", serviceToken)
                            .putMetadata("userAgent", userAgent)
                            .putMetadata("clientCity", clientCity)
                                    .putData("email", body.getOrDefault("email", ""))
                                    .putData("name", body.getOrDefault("name", ""))
                                    .putData("surname", body.getOrDefault("surname", ""))
                                    .putData("phoneNumber", body.getOrDefault("phoneNumber", ""))
                                    .putData("country", body.getOrDefault("country", ""))
                                    .putData("state", body.getOrDefault("state", ""))
                                    .putData("city", body.getOrDefault("city", ""))
                                    .putData("postalCode", body.getOrDefault("postalCode", ""))
                                    .putData("addressLine1", body.getOrDefault("addressLine1", ""))
                                    .putData("addressLine2", body.getOrDefault("addressLine2", ""))
                                    .putData("isDefault", body.getOrDefault("isDefault", ""))
                            .build();

                    return rSocketService.getRSocketRequester(Services.USER_SERVICE)
                                            .route("user.createUserDetails")
                                            .data(protoRequest)
                            .retrieveMono(ProtoResponse.class)
                            .flatMap(protoResponse -> {
                                Map<String, String> responseBody = createResponseBody(
                                        authentication, protoResponse);
                                HttpStatus status = HttpStatus.valueOf(protoResponse.getStatusCode());
                                return Mono.just(new ResponseEntity<>(responseBody, status));
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing user-details request", e);
                                return Mono.just(new ResponseEntity<>(
                                        Map.of("error", "Could not create user details"),
                                        HttpStatus.INTERNAL_SERVER_ERROR));
                            });
                });
    }

    private Map<String, String> createResponseBody(CustomAuthentication authentication,
            ProtoResponse protoResponse) {
        Map<String, String> responseBody = new HashMap<>();
        if (HttpStatus.OK.value() == protoResponse.getStatusCode()) {
            responseBody.put("userStatus", AuthResponseStatuses.AUTHORIZED_USER.name());
        } else {
            responseBody.put("userStatus", authentication.getUserStatus());
        }
        responseBody.put("status", protoResponse.getStatus());
        responseBody.put("message", protoResponse.getMessage());
        return responseBody;
    }

    private void setCookiesIfPresent(ServerWebExchange exchange, ProtoResponse protoResponse) {
        Optional.ofNullable(protoResponse.getMetadataOrDefault("accessToken", ""))
                .filter(token -> !token.isEmpty())
                .ifPresent(accessToken -> setCookie(exchange, "accessToken", accessToken,
                        Constants.ACCESS_TOKEN_COOKIE_LIFETIME));

        Optional.ofNullable(protoResponse.getMetadataOrDefault("sessionId", ""))
                .filter(sessionId -> !sessionId.isEmpty())
                .ifPresent(sessionId -> setCookie(exchange, "sessionId", sessionId,
                        Constants.SESSION_ID_LIFETIME));

        Optional.ofNullable(protoResponse.getMetadataOrDefault("refreshToken", ""))
                .filter(refreshToken -> !refreshToken.isEmpty())
                .ifPresent(refreshToken -> setCookie(exchange, "refreshToken", refreshToken,
                        Constants.REFRESH_TOKEN_COOKIE_LIFETIME));
    }

    private void setCookie(ServerWebExchange exchange, String name, String value, int maxAge) {
        exchange.getResponse().addCookie(ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(maxAge)
                .build());
    }
}