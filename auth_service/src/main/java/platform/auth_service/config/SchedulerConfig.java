package platform.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class SchedulerConfig {

    @Bean
    public Scheduler unboundedElastic() {
        return Schedulers.newBoundedElastic(
            Integer.MAX_VALUE,  
            Integer.MAX_VALUE,
            "unbounded-elastic"
        );
    }
}