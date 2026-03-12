package platform.monitoring_service.domain;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("monitoring_events")
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringEvent {
    @Id
    private Long id;
    private String eventType;
    private String serviceName;
    private String userId;
    private String userAgent;
    private String clientCity;
    private String details;
    private Json metadata;
    private LocalDateTime timestamp;

    public MonitoringEvent(String eventType, String serviceName, String userId,
            String userAgent, String clientCity, String details, LocalDateTime timestamp,
            Json metadata) {
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.userId = userId;
        this.userAgent = userAgent;
        this.clientCity = clientCity;
        this.details = details;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }
}
