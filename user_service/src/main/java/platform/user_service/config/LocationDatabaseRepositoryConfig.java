package platform.user_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "platform.user_service.repository.location", entityOperationsRef = "locationR2dbcEntityTemplate")
public class LocationDatabaseRepositoryConfig {
}
