package co.com.franquicias.model.exceptions;

import co.com.franquicias.model.enums.TechnicalMessage;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final TechnicalMessage technicalMessage;

    public BaseException(TechnicalMessage technicalMessage) {
        super(technicalMessage.getMessage());
        this.technicalMessage = technicalMessage;
    }
}
