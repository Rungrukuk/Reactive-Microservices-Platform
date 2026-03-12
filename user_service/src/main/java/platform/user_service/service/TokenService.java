package platform.user_service.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TokenService {
    public Mono<String> validateTokenAndGetUserId(Map<String, String> metadata,
            Destination destination);

    public enum Audience {
        USER_SERVICE
    }


    public enum Destination {
        CREATE_USER_DETAILS
    }
}
