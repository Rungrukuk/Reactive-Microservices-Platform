package platform.user_service.domain.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("cities")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class City {

    @Id
    private Integer id;
    private Integer stateId;
    private String name;
}
