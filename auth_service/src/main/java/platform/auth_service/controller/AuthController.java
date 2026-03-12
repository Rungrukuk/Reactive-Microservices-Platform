package platform.auth_service.controller;

import platform.auth_service.ProtoAuthRequest;
import platform.auth_service.ProtoAuthResponse;
import platform.auth_service.ProtoRequest;
import platform.auth_service.ProtoResponse;
import platform.auth_service.service.AuthService;
import platform.auth_service.service.MonitoringClient;
import platform.auth_service.service.ReactiveJwtHandler;
import platform.auth_service.service.UserService;
import platform.auth_service.service.ReactiveJwtHandler.Audience;
import platform.auth_service.service.ReactiveJwtHandler.Destination;
import platform.auth_service.util.EventType;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@MessageMapping("auth")
public class AuthController {

    private final UserService userService;

    private final ReactiveJwtHandler jwtValidationService;

    private final AuthService authService;

    private final MonitoringClient monitoringClient;

    // TODO add email verification
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(ProtoRequest request) {
        return jwtValidationService.validateServiceToken(request.getMetadataMap(), Destination.REGISTER).flatMap(
                claims -> {
                    return userService.createUser(request.getDataMap(), request.getMetadataMap())
                            .flatMap(userResponse -> {
                                return Mono.just(ProtoResponse.newBuilder()
                                        .setStatusCode(userResponse.getStatusCode())
                                        .setMessage(userResponse.getMessage())
                                        .setStatus(userResponse.getResponseStatus().name())
                                        .putMetadata("accessToken", userResponse.getAccessToken())
                                        .putMetadata("sessionId", userResponse.getSessionId())
                                        .putMetadata("refreshToken", userResponse.getRefreshToken())
                                        .putData("email", userResponse.getEmail())
                                        .build());
                            });
                }).switchIfEmpty(Mono.defer(() -> {
                    monitoringClient.sendEvent(EventType.SERVICE_TOKEN_MISMATCH,
                            Audience.AUTH_SERVICE.name(),
                            "",
                            request.getMetadataMap().getOrDefault("userAgent", ""),
                            request.getMetadataMap().getOrDefault("clientCity", ""),
                            "Invalid service token in register", request.getMetadataMap());
                    return Mono.just(
                            ProtoResponse.newBuilder()
                                    .setStatusCode(403)
                                    .setStatus("Forbidden")
                                    .setMessage("No required permissions")
                                    .build());
                }));
    }

    @MessageMapping("validateToken")
    public Mono<ProtoAuthResponse> validateAndIssueNewToken(ProtoAuthRequest request) {
        return authService.validate(request).map(authResponse -> {
            ProtoAuthResponse protoAuthResponse = ProtoAuthResponse.newBuilder()
                    .setStatus(authResponse.getResponseStatus().name())
                    .setStatusCode(authResponse.getStatusCode())
                    .putMetadata("accessToken", authResponse.getAccessToken())
                    .putMetadata("sessionId", authResponse.getSessionId())
                    .putMetadata("serviceToken", authResponse.getServiceToken())
                    .putMetadata("refreshToken", authResponse.getRefreshToken())
                    .build();
            return protoAuthResponse;
        });
    }

    // TODO add email verification
    @MessageMapping("loginUser")
    public Mono<ProtoResponse> loginUser(ProtoRequest request) {
        return jwtValidationService.validateServiceToken(request.getMetadataMap(), Destination.LOGIN)
                .flatMap(claims -> {
                    return userService.authenticateUser(request.getDataMap(), request.getMetadataMap())
                            .flatMap(userResponse -> {
                                return Mono.just(ProtoResponse.newBuilder()
                                        .setStatusCode(userResponse.getStatusCode())
                                        .setMessage(userResponse.getMessage())
                                        .setStatus(userResponse.getResponseStatus().name())
                                        .putMetadata("accessToken", userResponse.getAccessToken())
                                        .putMetadata("sessionId", userResponse.getSessionId())
                                        .putMetadata("refreshToken", userResponse.getRefreshToken())
                                        .putData("email", userResponse.getEmail())
                                        .build());
                            });

                })
                .switchIfEmpty(Mono.defer(() -> {
                    monitoringClient.sendEvent(EventType.SERVICE_TOKEN_MISMATCH,
                            Audience.AUTH_SERVICE.name(),
                            "",
                            request.getMetadataMap().getOrDefault("userAgent", ""),
                            request.getMetadataMap().getOrDefault("clientCity", ""),
                            "Invalid service token in login", request.getMetadataMap());
                    return Mono.just(
                            ProtoResponse.newBuilder()
                                    .setStatusCode(403)
                                    .setStatus("Forbidden")
                                    .setMessage("No required permissions")
                                    .build());
                }));
    }
}
