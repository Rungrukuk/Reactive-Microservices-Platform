package platform.user_service.service.implementation;

import platform.user_service.domain.user.Address;
import platform.user_service.domain.user.User;
import platform.user_service.domain.user.UserAddress;
import platform.user_service.dto.UserResponse;
import platform.user_service.repository.user.AddressRepository;
import platform.user_service.repository.user.UserAddressRepository;
import platform.user_service.service.InputValidatorService;
import platform.user_service.service.UserService;
import platform.user_service.util.CustomResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final InputValidatorService inputValidatorService;

    @Qualifier("userTransactionalOperator")
    private final TransactionalOperator transactionalOperator;

    private final AddressRepository addressRepository;

    private final UserAddressRepository userAddressRepository;

    @Qualifier("userR2dbcEntityTemplate")
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<UserResponse> createUserDetails(Map<String, String> data) {
        return inputValidatorService.validateInput(data)
                .flatMap(errors -> {
                    if (!errors.isEmpty()) {
                        return createBadRequestResponse(errors.toString());
                    }
                    String userId = data.get("userId");
                    boolean shouldBeDefault = Boolean.parseBoolean(data.get("isDefault"));
                    User user = buildUserFromData(data);
                    Address address = buildAddressFromData(data);

                    return userExists(userId)
                            .flatMap(exists -> {
                                if (exists) {
                                    return createAddressFlow(userId, address, shouldBeDefault);
                                }

                                return r2dbcEntityTemplate.insert(User.class)
                                        .using(user)
                                        .flatMap(savedUser -> createAddressFlow(
                                                savedUser.getId(),
                                                address,
                                                shouldBeDefault));
                            })
                            .thenReturn(buildSuccessResponse())
                            .as(transactionalOperator::transactional)
                            .onErrorResume(this::handleError);
                })
                .onErrorResume(this::handleError);
    }

    private Mono<Boolean> userExists(String userId) {
        return r2dbcEntityTemplate
                .selectOne(
                        Query.query(Criteria.where("id").is(userId)),
                        User.class)
                .hasElement();
    }

    private Mono<Void> createAddressFlow(
            String userId,
            Address address,
            boolean shouldBeDefault) {
        return addressRepository.save(address)
                .flatMap(savedAddress -> createUserAddressEntry(
                        userId,
                        savedAddress.getId(),
                        shouldBeDefault))
                .then();
    }

    private Mono<UserAddress> createUserAddressEntry(
            String userId,
            Long addressId,
            boolean shouldBeDefault) {
        if (shouldBeDefault) {
            return userAddressRepository
                    .unsetDefaultByUserId(userId)
                    .then(saveUserAddress(userId, addressId, true));
        }

        return saveUserAddress(userId, addressId, false);
    }

    private Mono<UserAddress> saveUserAddress(
            String userId,
            Long addressId,
            boolean isDefault) {
        UserAddress ua = new UserAddress();
        ua.setUserId(userId);
        ua.setAddressId(addressId);
        ua.setDefault(isDefault);
        return userAddressRepository.save(ua);
    }

    private User buildUserFromData(Map<String, String> data) {
        User user = new User();
        user.setId(data.get("userId"));
        user.setName(data.get("name"));
        user.setSurname(data.get("surname"));
        user.setPhoneNumber(data.get("phoneNumber"));
        return user;
    }

    private Address buildAddressFromData(Map<String, String> data) {
        Address address = new Address();
        address.setCountry(data.get("country"));
        address.setState(data.get("state"));
        address.setCity(data.get("city"));
        address.setPostalCode(data.get("postalCode"));
        address.setAddressLine_1(data.get("addressLine1"));
        address.setAddressLine_2(data.get("addressLine2"));
        return address;
    }

    private Mono<UserResponse> createBadRequestResponse(String message) {
        return Mono.just(
                new UserResponse(CustomResponseStatus.BAD_REQUEST, message, 400));
    }

    private Mono<UserResponse> handleError(Throwable e) {
        log.error("Unexpected Error",e);
        return Mono.just(
                new UserResponse(
                        CustomResponseStatus.UNEXPECTED_ERROR,
                        "Unexpected error",
                        500));
    }

    private UserResponse buildSuccessResponse() {
        return new UserResponse(
                CustomResponseStatus.OK,
                "User details created successfully",
                200);
    }

    @Override
    public Mono<UserResponse> getUserDetails(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'getUserDetails'");
    }

    @Override
    public Mono<UserResponse> deleteUserDetails(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteUserDetails'");
    }

    @Override
    public Mono<UserResponse> updateUserDetails(UserResponse user) {
        throw new UnsupportedOperationException("Unimplemented method 'updateUserDetails'");
    }
}
