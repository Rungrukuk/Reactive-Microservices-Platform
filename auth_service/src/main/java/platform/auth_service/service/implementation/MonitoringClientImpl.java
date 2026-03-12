package platform.auth_service.service.implementation;

import platform.auth_service.ProtoMonitoringEvent;
import platform.auth_service.service.MonitoringClient;
import platform.auth_service.util.EventType;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringClientImpl implements MonitoringClient {

    @Value("${services.monitoring.host:localhost}")
    private String monitoringHost;

    @Value("${services.monitoring.port:7002}")
    private int monitoringPort;

    @Value("${client.ssl.bundle:}")
    private String sslBundleName;

    @Value("${client.ssl.enabled:false}")
    private boolean sslEnabled;

    private final RSocketRequester.Builder requesterBuilder;
    private final SslBundles sslBundles;

    private RSocketRequester requester;

    private final Scheduler unboundedElastic;

    @PostConstruct
    public void init() {
        try {
            TcpClient tcpClient = buildTcpClient();
            requester = requesterBuilder
                    .rsocketConnector(connector -> connector
                            .reconnect(Retry.fixedDelay(5, Duration.ofSeconds(2))))
                    .transport(TcpClientTransport.create(tcpClient));

            log.info("MonitoringClient connected to {}:{}", monitoringHost, monitoringPort);
        } catch (Exception e) {
            log.error("MonitoringClient initialization failed — monitoring events will be dropped", e);
        }
    }

    private TcpClient buildTcpClient() throws Exception {
        TcpClient tcpClient = TcpClient.create()
                .host(monitoringHost)
                .port(monitoringPort)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        if (!sslEnabled || sslBundleName == null || sslBundleName.isEmpty()) {
            log.warn("MonitoringClient SSL is disabled — connection will be unencrypted");
            return tcpClient;
        }

        SslBundle bundle = sslBundles.getBundle(sslBundleName);
        KeyManagerFactory kmf = bundle.getManagers().getKeyManagerFactory();
        TrustManagerFactory tmf = bundle.getManagers().getTrustManagerFactory();

        SslContext nettySslContext = SslContextBuilder.forClient()
                .keyManager(kmf)
                .trustManager(tmf)
                .build();

        return tcpClient.secure(ssl -> ssl.sslContext(nettySslContext));
    }

    @Override
    public Mono<Void> sendEvent(
            EventType eventType,
            String serviceName,
            String userId,
            String userAgent,
            String clientCity,
            String details,
            Map<String, String> metadata) {

        if (requester == null) {
            log.warn("MonitoringClient not initialized — dropping event: {}", eventType);
            return Mono.empty();
        }

        Mono<Void> pipeline = Mono.fromCallable(() -> {
                    ProtoMonitoringEvent.Builder builder = ProtoMonitoringEvent.newBuilder()
                            .setEventType(eventType.name())
                            .setServiceName(serviceName)
                            .setUserId(userId != null ? userId : "")
                            .setUserAgent(userAgent != null ? userAgent : "")
                            .setClientCity(clientCity != null ? clientCity : "")
                            .setDetails(details != null ? details : "")
                            .setTimestamp(Instant.now().toString());

                    if (metadata != null && !metadata.isEmpty()) {
                        builder.putAllMetadata(metadata);
                    }
                    return builder.build();
                })
                .flatMap(event -> requester
                        .route("monitoring.logEvent")
                        .data(event)
                        .send())
                .onErrorResume(e -> {
                    log.warn("Monitoring event dropped [{}]: {}", eventType, e.getMessage());
                    return Mono.empty();
                });

        // Fire-and-forget: subscribe on a worker thread without blocking the main request flow.
        pipeline.subscribeOn(unboundedElastic).subscribe();
        return Mono.empty();
    }
}