package co.com.franquicias.model.exceptions;

import co.com.franquicias.model.enums.TechnicalMessage;

public class TechnicalException extends BaseException {
    public TechnicalException(Throwable cause) {
        super(TechnicalMessage.TECHNICAL_ERROR);
        initCause(cause);
    }
}
