package platform.monitoring_service.controller;

import platform.monitoring_service.ProtoMonitoringEvent;
import platform.monitoring_service.service.MonitoringService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @MessageMapping("monitoring.logEvent")
    public Mono<Void> logEvent(ProtoMonitoringEvent event) {
        return monitoringService.handleEvent(event);
    }
}
