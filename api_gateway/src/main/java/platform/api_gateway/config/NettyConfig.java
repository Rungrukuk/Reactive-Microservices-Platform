package platform.api_gateway.config;

import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.channel.ChannelOption;

@Configuration
public class NettyConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> {
            factory.addServerCustomizers(
                httpServer -> httpServer
                    .maxConnections(-1)
                    .option(ChannelOption.SO_BACKLOG, Integer.MAX_VALUE)
            );
        };
    }
}