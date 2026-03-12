package platform.user_service.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager userTransactionManager(
            @Qualifier("userConnectionFactory") ConnectionFactory userConnectionFactory) {
        return new R2dbcTransactionManager(userConnectionFactory);
    }

    @Bean
    public ReactiveTransactionManager locationTransactionManager(
            @Qualifier("locationConnectionFactory") ConnectionFactory locationConnectionFactory) {
        return new R2dbcTransactionManager(locationConnectionFactory);
    }

    @Bean
    public TransactionalOperator userTransactionalOperator(
            @Qualifier("userTransactionManager")
            ReactiveTransactionManager userTransactionManager) {
        return TransactionalOperator.create(userTransactionManager);
    }

    @Bean
    public TransactionalOperator locationTransactionalOperator(
            @Qualifier("locationTransactionManager")
            ReactiveTransactionManager locationTransactionManager) {
        return TransactionalOperator.create(locationTransactionManager);
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.r2dbc.user")
    public R2dbcProperties userR2dbcProperties() {
        return new R2dbcProperties();
    }

    @Bean
    @Primary
    public ConnectionFactory userConnectionFactory(
            @Qualifier("userR2dbcProperties") R2dbcProperties userProperties) {
        return ConnectionFactoryBuilder.withUrl(userProperties.getUrl())
                .username(userProperties.getUsername())
                .password(userProperties.getPassword())
                .build();
    }

    @Bean
    @ConfigurationProperties("spring.r2dbc.location")
    public R2dbcProperties locationR2dbcProperties() {
        return new R2dbcProperties();
    }

    @Bean
    public ConnectionFactory locationConnectionFactory(
            @Qualifier("locationR2dbcProperties") R2dbcProperties locationProperties) {
        return ConnectionFactoryBuilder.withUrl(locationProperties.getUrl())
                .username(locationProperties.getUsername())
                .password(locationProperties.getPassword())
                .build();
    }

    @Bean
    public R2dbcEntityTemplate userR2dbcEntityTemplate(
            @Qualifier("userConnectionFactory") ConnectionFactory userConnectionFactory) {
        return new R2dbcEntityTemplate(userConnectionFactory);
    }

    @Bean
    public R2dbcEntityTemplate locationR2dbcEntityTemplate(
            @Qualifier("locationConnectionFactory") ConnectionFactory locationConnectionFactory) {
        return new R2dbcEntityTemplate(locationConnectionFactory);
    }
}
