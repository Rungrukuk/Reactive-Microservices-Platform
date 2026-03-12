package platform.auth_service.dto;

import platform.auth_service.util.CustomResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthResponse extends BaseResponse {
    private String serviceToken = "";
    private String refreshToken = "";
    private CustomResponseStatus responseStatus;
    private int statusCode = 500;

    public AuthResponse(String accessToken, String refreshToken, String sessionId,
            String serviceToken,
            CustomResponseStatus responseStatus, int statusCode) {
        super(accessToken, sessionId);
        this.serviceToken = serviceToken;
        this.refreshToken = refreshToken;
        this.statusCode = statusCode;
        this.responseStatus = responseStatus;
    }

}
