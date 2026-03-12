package platform.auth_service.service.implementation;

import platform.auth_service.domain.User;
import platform.auth_service.dto.UserDTO;
import platform.auth_service.dto.UserResponse;
import platform.auth_service.repository.SessionRepository;
import platform.auth_service.repository.UserRepository;
import platform.auth_service.security.InputValidator;
import platform.auth_service.service.MonitoringClient;
import platform.auth_service.service.ReactiveJwtHandler;
import platform.auth_service.service.RefreshTokenService;
import platform.auth_service.service.UserService;
import platform.auth_service.util.CustomResponseStatus;
import platform.auth_service.util.EventType;
import platform.auth_service.util.Roles;
import platform.auth_service.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final SessionRepository sessionRepository;
    private final TransactionalOperator transactionalOperator;
    private final ReactiveJwtHandler jwtHandler;
    private final Argon2PasswordEncoder passwordEncoder;
    private final InputValidator validatorService;
    private final RefreshTokenService refreshTokenService;
    private final MonitoringClient monitoringClient;
    private final Scheduler unboundedElastic;

    @Override
    public Mono<UserResponse> createUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String rePassword = data.get("rePassword");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateInput(email, password, rePassword)
                .flatMap(errors -> !errors.isEmpty()
                        ? badRequest(errors.toString())
                        : userRepository.findUserDtoByEmail(email)
                                .flatMap(__ -> badRequest("User already exists"))
                                .switchIfEmpty(
                                        encodePassword(password)
                                                .flatMap(encodedPassword ->
                                                        registerUser(email, encodedPassword, userAgent, clientCity)
                                                                .as(transactionalOperator::transactional))))
                .onErrorResume(this::handleUnexpectedError);
    }

    @Override
    public Mono<UserResponse> authenticateUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateInput(email, password)
                .flatMap(errors -> {
                    if (!errors.isEmpty()) {
                        return badRequest(errors.toString());
                    }
                    return userRepository.findUserByEmail(email)
                            .flatMap(user ->
                                    checkPassword(password, user)
                                            .flatMap(matches -> {
                                                if (!matches) {
                                                    monitoringClient.sendEvent(
                                                            EventType.FAILED_LOGIN_ATTEMPT,
                                                            "AUTH_SERVICE",
                                                            user.getUserId(),
                                                            userAgent,
                                                            clientCity,
                                                            email,
                                                            metadata);
                                                    return badRequest("Email or password is incorrect");
                                                }
                                                return prepareTokens(user.getUserId(), Roles.USER.name())
                                                        .flatMap(bundle ->
                                                                persistTokens(user.getUserId(), email, bundle,
                                                                        userAgent, clientCity, "Logged in successfully")
                                                                        .as(transactionalOperator::transactional));
                                            }))
                            .switchIfEmpty(Mono.defer(() -> {
                                monitoringClient.sendEvent(
                                        EventType.FAILED_LOGIN_ATTEMPT,
                                        "AUTH_SERVICE",
                                        "",
                                        userAgent,
                                        clientCity,
                                        email,
                                        metadata);
                                return badRequest("Email or password is incorrect");
                            }));
                })
                .onErrorResume(this::handleUnexpectedError);
    }

    @Override
    public Mono<UserDTO> getUser(String userId) {
        throw new UnsupportedOperationException("getUser not yet implemented");
    }

    @Override
    public Mono<UserDTO> deleteUser(String userId) {
        throw new UnsupportedOperationException("deleteUser not yet implemented");
    }

    @Override
    public Mono<UserDTO> updateUser(UserDTO user) {
        throw new UnsupportedOperationException("updateUser not yet implemented");
    }

    private Mono<UserResponse> registerUser(String email, String encodedPassword,
            String userAgent, String clientCity) {
        User newUser = new User(email, encodedPassword, Roles.USER.name());
        return prepareTokens(newUser.getUserId(), Roles.USER.name())
                .flatMap(bundle ->
                        Mono.defer(() -> entityTemplate.insert(User.class).using(newUser)
                                        .flatMap(__ ->
                                                persistTokens(newUser.getUserId(), email, bundle,
                                                        userAgent, clientCity, "User created successfully")))
                                .as(transactionalOperator::transactional));
    }

    private Mono<TokenBundle> prepareTokens(String userId, String role) {
        return Mono.zip(
                        jwtHandler.createAccessToken(userId, role),
                        jwtHandler.createRefreshToken(userId, role))
                .flatMap(tokens -> {
                    String accessToken = tokens.getT1();
                    String refreshToken = tokens.getT2();
                    return encodeRefreshToken(refreshToken)
                            .map(encodedRefreshToken ->
                                    new TokenBundle(accessToken, refreshToken, encodedRefreshToken));
                });
    }

    private Mono<UserResponse> persistTokens(String userId, String email, TokenBundle bundle,
            String userAgent, String clientCity, String message) {
        return Mono.zip(
                        refreshTokenService.createOrUpdateRefreshTokenEntity(
                                userId, bundle.encodedRefreshToken(), userAgent, clientCity),
                        sessionRepository.saveSession(bundle.accessToken()))
                .map(tuple -> buildSuccessResponse(
                        email,
                        bundle.accessToken(),
                        bundle.refreshToken(),
                        tuple.getT2().getSessionId(),
                        message))
                .doOnError(e ->
                        log.error("Failed to persist tokens for userId={}: {}", userId, e.getMessage(), e));
    }

    private Mono<String> encodePassword(String rawPassword) {
        return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                .subscribeOn(unboundedElastic);
    }

    private Mono<String> encodeRefreshToken(String rawRefreshToken) {
        return Mono.fromCallable(() -> passwordEncoder.encode(TokenHashUtil.hash(rawRefreshToken)))
                .subscribeOn(unboundedElastic);
    }

    private Mono<Boolean> checkPassword(String rawPassword, User user) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .subscribeOn(unboundedElastic);
    }

    private Mono<List<String>> validateInput(String email, String password, String rePassword) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password, rePassword));
    }

    private Mono<List<String>> validateInput(String email, String password) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password));
    }

    private UserResponse buildSuccessResponse(String email, String accessToken,
            String refreshToken, String sessionId, String message) {
        UserResponse response = new UserResponse();
        response.setEmail(email);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setSessionId(sessionId);
        response.setStatusCode(200);
        response.setMessage(message);
        response.setResponseStatus(CustomResponseStatus.OK);
        return response;
    }

    private Mono<UserResponse> badRequest(String message) {
        UserResponse response = new UserResponse();
        response.setMessage(message);
        response.setResponseStatus(CustomResponseStatus.BAD_REQUEST);
        response.setStatusCode(400);
        return Mono.just(response);
    }

    private Mono<UserResponse> handleUnexpectedError(Throwable e) {
        log.error("Unexpected error in UserService: {}", e.getMessage(), e);
        UserResponse response = new UserResponse();
        response.setMessage("Unexpected error");
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        response.setStatusCode(500);
        return Mono.just(response);
    }

    private record TokenBundle(String accessToken, String refreshToken, String encodedRefreshToken) {}
}