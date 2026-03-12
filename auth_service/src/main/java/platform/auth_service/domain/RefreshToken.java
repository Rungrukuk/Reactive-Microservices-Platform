package platform.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken {
    @Id
    private UUID id;
    private String userId;
    private String refreshToken;
    private String userAgent;
    private String clientCity;

    public RefreshToken(String userId, String refreshToken, String userAgent, String clientCity) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.userAgent = userAgent;
        this.clientCity = clientCity;
    }
}
