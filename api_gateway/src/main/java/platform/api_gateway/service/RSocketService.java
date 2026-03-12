package platform.api_gateway.service;

import platform.api_gateway.config.ServiceConfigProperties;
import platform.api_gateway.util.Services;
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

import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RSocketService {

    private final RSocketRequester.Builder requesterBuilder;
    private final ServiceConfigProperties serviceConfigs;
    private final SslBundles sslBundles;

    private final Map<Services, List<RSocketRequester>> requesterPool = new ConcurrentHashMap<>();
    private final Map<Services, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    @Value("${client.ssl.bundle:}")
    private String sslBundleName;

    @Value("${client.ssl.enabled:false}")
    private boolean sslEnabled;

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    @PostConstruct
    public void init() {
        try {
            TcpClient baseTcpClient = buildTcpClient();

            for (Map.Entry<String, ServiceConfigProperties.ServiceEndpoint> entry
                    : serviceConfigs.getEndpoints().entrySet()) {

                Services service = Services.valueOf(entry.getKey());
                ServiceConfigProperties.ServiceEndpoint endpoint = entry.getValue();

                List<RSocketRequester> pool = new ArrayList<>();

                for (int i = 0; i < POOL_SIZE; i++) {
                    TcpClient tcpClient = baseTcpClient
                            .host(endpoint.getHost())
                            .port(endpoint.getPort());

                    RSocketRequester requester = requesterBuilder
                            .rsocketConnector(connector -> connector
                                    .reconnect(Retry.fixedDelay(5, Duration.ofSeconds(2)))
                                    .keepAlive(Duration.ofSeconds(20), Duration.ofSeconds(90)))
                            .transport(TcpClientTransport.create(tcpClient));

                    pool.add(requester);
                }

                requesterPool.put(service, pool);
                roundRobinCounters.put(service, new AtomicInteger(0));

                log.info("Created {} RSocket connections for {} at {}:{}",
                        POOL_SIZE, service, endpoint.getHost(), endpoint.getPort());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise RSocket connections with mTLS", e);
        }
    }

    public RSocketRequester getRSocketRequester(Services service) {
        List<RSocketRequester> pool = requesterPool.get(service);
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException(
                    "No RSocket requesters configured for service: " + service);
        }
        int index = roundRobinCounters.get(service)
                .getAndIncrement() % pool.size();
        return pool.get(Math.abs(index));
    }

    private TcpClient buildTcpClient() throws Exception {
        if (!sslEnabled || sslBundleName == null || sslBundleName.isEmpty()) {
            log.warn("RSocket client SSL is disabled — connections will be unencrypted");
            return TcpClient.create();
        }

        SslBundle bundle = sslBundles.getBundle(sslBundleName);
        KeyManagerFactory kmf = bundle.getManagers().getKeyManagerFactory();
        TrustManagerFactory tmf = bundle.getManagers().getTrustManagerFactory();

        SslContext nettySslContext = SslContextBuilder.forClient()
                .keyManager(kmf)
                .trustManager(tmf)
                .build();

        return TcpClient.create()
                .secure(ssl -> ssl.sslContext(nettySslContext));
    }
}