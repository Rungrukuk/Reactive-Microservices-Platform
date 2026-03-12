package platform.user_service.repository.location;

import platform.user_service.domain.location.State;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface StateRepository extends ReactiveCrudRepository<State, Integer> {

    @Query("SELECT * FROM states WHERE name = :name AND (:countryId IS NULL OR country_id = :countryId)")
    Mono<State> findByNameAndCountryId(String state, Integer id);

    Mono<State> findByName(String state);

}
