package platform.user_service.domain.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("countries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Country {

    @Id
    private Integer id;
    private String name;
    private String iso3;
    private String iso2;

}
