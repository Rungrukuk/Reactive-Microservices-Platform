package platform.monitoring_service.service;



import platform.monitoring_service.ProtoMonitoringEvent;
import platform.monitoring_service.domain.MonitoringEvent;
import platform.monitoring_service.repository.MonitoringEventRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MonitoringEventRepository eventRepository;

    public Mono<Void> handleEvent(ProtoMonitoringEvent event) {
        return Mono.fromCallable(() -> {
                    String metadataJson =
                            (event.getMetadataMap() == null || event.getMetadataMap().isEmpty())
                                    ? "{}"
                                    : objectMapper.writeValueAsString(event.getMetadataMap());

                    Json metadata = Json.of(metadataJson);

                    return new MonitoringEvent(
                            event.getEventType(),
                            event.getServiceName(),
                            event.getUserId(),
                            event.getUserAgent(),
                            event.getClientCity(),
                            event.getDetails(),
                            convertToLocalDateTime(event.getTimestamp()),
                            metadata);
                })
                .flatMap(eventRepository::save)
                .doOnSuccess(saved -> log.warn(
                        "[SUSPICIOUS EVENT] id={} type={} service={} userId={} details={}",
                        saved.getId(),
                        saved.getEventType(),
                        saved.getServiceName(),
                        saved.getUserId(),
                        saved.getDetails()))
                .doOnError(
                        e -> log.error("Failed to persist monitoring event: {}", e.getMessage(), e))
                .then();
    }

    private LocalDateTime convertToLocalDateTime(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            Instant instant = Instant.parse(timestampStr);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            log.warn("Could not string to timestamp, using  Instant.now()", e);
            return LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        }
    }

}
