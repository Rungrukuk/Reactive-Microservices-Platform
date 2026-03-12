package platform.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String userId;
    private String email;
    private String password;
    private String roleName;

    public User(String email, String password, String roleName) {
        this.userId = UUID.randomUUID().toString();
        this.email = email;
        this.password = password;
        this.roleName = roleName;
    }
}
