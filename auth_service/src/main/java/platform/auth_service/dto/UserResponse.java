package platform.auth_service.dto;

import platform.auth_service.util.CustomResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserResponse extends BaseResponse {

    private String email = "";
    private String refreshToken = "";
    private CustomResponseStatus responseStatus = CustomResponseStatus.UNEXPECTED_ERROR;
    private int statusCode = 500;
    private String message = "";

    public UserResponse(String email, String accessToken, String sessionId, String refreshToken,
            CustomResponseStatus responseStatus,
            String message) {
        super(accessToken, sessionId);
        this.email = email;
        this.refreshToken = refreshToken;
        this.message = message;
        this.responseStatus = responseStatus;
    }
}
