package platform.user_service.service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface InputValidatorService {
    public Mono<List<String>> validateInput(Map<String, String> input);
}
