package platform.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("roles")
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    private String roleName;
}
