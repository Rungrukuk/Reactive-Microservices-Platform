package platform.user_service.controller;

import platform.auth_service.RequestProto.ProtoRequest;
import platform.auth_service.ResponseProto.ProtoResponse;
import platform.user_service.service.TokenService;
import platform.user_service.service.TokenService.Destination;
import platform.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@MessageMapping("user")
public class UserController {

    private final TokenService tokenService;

    private final UserService userService;

    @MessageMapping("createUserDetails")
    public Mono<ProtoResponse> createUser(ProtoRequest request) {

        return tokenService.validateTokenAndGetUserId(
                        request.getMetadataMap(),
                        Destination.CREATE_USER_DETAILS)
                .flatMap(userId -> {

                    Map<String, String> data = new HashMap<>(request.getDataMap());
                    data.put("userId", userId);

                    return userService.createUserDetails(data)
                            .map(userResponse -> ProtoResponse.newBuilder()
                                    .setStatusCode(userResponse.getStatusCode())
                                    .setMessage(userResponse.getMessage())
                                    .setStatus(userResponse.getResponseStatus()
                                            .name())
                                    .build());
                })
                .switchIfEmpty(Mono.just(
                        ProtoResponse.newBuilder()
                                .setStatusCode(403)
                                .setMessage("No required permission")
                                .setStatus("Forbidden")
                                .build()));
    }

}
