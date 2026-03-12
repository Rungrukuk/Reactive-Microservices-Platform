package platform.user_service.service;

import platform.user_service.util.EventType;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MonitoringClient {
    Mono<Void> sendEvent(
            EventType eventType,
            String serviceName,
            String userId,
            String userAgent,
            String clientCity,
            String details,
            Map<String, String> metadata);
}
