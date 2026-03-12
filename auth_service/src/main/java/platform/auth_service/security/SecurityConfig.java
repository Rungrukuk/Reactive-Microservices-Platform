package platform.auth_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public Argon2PasswordEncoder passwordEncoder() {
        int saltLength  = 16;
        int hashLength  = 32;
        int parallelism = 4;
        int memory      = 8192;
        int iterations  = 3; 

        return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory, iterations);
    }

}
