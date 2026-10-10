package co.com.franquicias.model.exceptions;

import co.com.franquicias.model.enums.TechnicalMessage;

public class BusinessException extends BaseException {
    public BusinessException(TechnicalMessage technicalMessage) {
        super(technicalMessage);
    }
}
