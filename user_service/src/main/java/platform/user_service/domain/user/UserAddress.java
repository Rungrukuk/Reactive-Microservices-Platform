package platform.user_service.domain.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_addresses")
public class UserAddress {
    @Id
    private String id;
    private String userId;
    private Long addressId;
    private boolean isDefault;
}
