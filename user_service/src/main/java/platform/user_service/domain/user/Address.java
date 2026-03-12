package platform.user_service.domain.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("addresses")
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    private Long id;
    private String country;
    private String state;
    private String city;
    private String postalCode;
    private String addressLine_1;
    private String addressLine_2;
}
