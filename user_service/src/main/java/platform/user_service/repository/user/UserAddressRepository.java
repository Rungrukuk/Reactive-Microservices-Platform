package platform.user_service.repository.user;

import platform.user_service.domain.user.UserAddress;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserAddressRepository extends ReactiveCrudRepository<UserAddress, String> {
    Mono<UserAddress> findByUserIdAndIsDefault(String userId, boolean isDefault);

    @Modifying
    @Query("UPDATE user_addresses SET is_default = false WHERE user_id = :userId")
    Mono<Void> unsetDefaultByUserId(@Param("userId") String userId);

}
