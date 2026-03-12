package platform.user_service.repository.location;

import platform.user_service.domain.location.Country;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CountryRepository extends ReactiveCrudRepository<Country, Integer> {
    Mono<Country> findByIso3(String iso3);
}
