package co.com.franquicias.usecase;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

public final class NameValidator {

    private NameValidator() {
    }

    /** Emits the trimmed name, or INVALID_NAME when it is null or blank. */
    public static Mono<String> validName(String name) {
        return Mono.justOrEmpty(name)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_NAME)));
    }

    /** True when no existing name matches the candidate, ignoring case. */
    public static boolean isAvailable(Stream<String> existingNames, String candidate) {
        return existingNames.noneMatch(candidate::equalsIgnoreCase);
    }
}
