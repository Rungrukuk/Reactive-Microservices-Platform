package platform.user_service.repository.location;

import platform.user_service.domain.location.City;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CityRepository extends ReactiveCrudRepository<City, Integer> {
    Flux<City> findByStateId(Integer stateId);

    Mono<City> findByNameAndStateId(String city, Integer id);

    Mono<City> findByName(String city);
}
