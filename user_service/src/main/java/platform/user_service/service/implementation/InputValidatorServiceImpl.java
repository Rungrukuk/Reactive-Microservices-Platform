package platform.user_service.service.implementation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import platform.user_service.repository.location.CityRepository;
import platform.user_service.repository.location.CountryRepository;
import platform.user_service.repository.location.PostalCodeRepository;
import platform.user_service.repository.location.StateRepository;
import platform.user_service.service.InputValidatorService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
// TODO need to implement max char size validation in both user and auth service
@RequiredArgsConstructor
public class InputValidatorServiceImpl implements InputValidatorService {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    private final CountryRepository countryRepository;

    private final StateRepository stateRepository;

    private final CityRepository cityRepository;

    private final PostalCodeRepository postalCodeRepository;

    private static final Pattern EMAIL_PATTERN = 
    Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z ]+$");

    @Override
    public Mono<List<String>> validateInput(Map<String, String> input) {
        String userId = input.get("userId");
        String email = input.get("email");
        String name = input.get("name");
        String surname = input.get("surname");
        String phoneNumber = input.get("phoneNumber");
        String country = input.get("country");
        String state = input.get("state");
        String city = input.get("city");
        String postalCode = input.get("postalCode");
        String addressLine1 = input.get("addressLine1");

        return Mono.fromCallable(() -> {
                    List<String> errors = new ArrayList<>();
                    if (userId == null || userId.isEmpty()) errors.add("User ID cannot be null or empty");
                    if (!isValidEmail(email)) errors.add("Invalid or empty email");
                    if (!isValidName(name)) errors.add("Invalid or empty name");
                    if (!isValidName(surname)) errors.add("Invalid or empty surname");
                    if (!isValidPhoneNumber(phoneNumber, country)) errors.add("Invalid or empty phone number");
                    if (addressLine1 == null || addressLine1.isEmpty()) errors.add("Address Line 1 cannot be empty");
                    return errors;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(errors -> validateLocation(country, state, city, postalCode)
                        .map(locationErrors -> {
                            errors.addAll(locationErrors);
                            return errors;
                        }));
    }

    private Mono<List<String>> validateLocation(String country, String state, String city,
            String postalCode) {
    List<String> locationErrors = new ArrayList<>();

    Mono<Void> countryCheck = countryRepository.findByIso3(country)
            .switchIfEmpty(Mono.defer(() -> {
                    locationErrors.add("Country not found: " + country);
                    return Mono.empty();
            }))
            .then();

    Mono<Void> stateCheck = (state == null || state.isEmpty())
            ? Mono.empty()
            : stateRepository.findByName(state)
                    .switchIfEmpty(Mono.defer(() -> {
                            locationErrors.add("State not found: " + state);
                            return Mono.empty();
                    }))
                    .then();

    Mono<Void> cityCheck = cityRepository.findByName(city)
            .switchIfEmpty(Mono.defer(() -> {
                    locationErrors.add("City not found: " + city);
                    return Mono.empty();
            }))
            .then();

    Mono<Void> postalCheck = postalCodeRepository.findByPostalCode(postalCode)
            .switchIfEmpty(Mono.defer(() -> {
                    locationErrors.add("Postal code not found: " + postalCode);
                    return Mono.empty();
            }))
            .then();

    return Mono.when(countryCheck, stateCheck, cityCheck, postalCheck)
            .thenReturn(locationErrors);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidName(String name) {
        if (name == null) return false;
        return NAME_PATTERN.matcher(name).matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber, String countryCode) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return false;
            }
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, countryCode);
            return phoneNumberUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
