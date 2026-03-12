package platform.api_gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceConfigProperties {

    private Map<String, ServiceEndpoint> endpoints;


    @Data
    public static class ServiceEndpoint {
        private String host;
        private int port;
    }
}
