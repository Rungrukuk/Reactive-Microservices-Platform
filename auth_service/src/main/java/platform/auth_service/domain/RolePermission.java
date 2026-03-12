package platform.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("role_permissions")
public class RolePermission {
    private String roleName;
    private Long permissionId;
}
