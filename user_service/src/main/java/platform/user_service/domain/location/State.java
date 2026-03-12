package platform.user_service.domain.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("states")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class State {
    @Id
    private Integer id;
    private Integer countryId;
    private String name;
    private String stateCode;
}
