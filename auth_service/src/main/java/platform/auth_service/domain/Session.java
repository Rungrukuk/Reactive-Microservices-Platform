package platform.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash("sessions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session implements Serializable {
    @Id
    private String accessToken;
    private String sessionId;
}
