package co.com.franquicias.api.error;

import co.com.franquicias.model.enums.TechnicalMessage;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(TechnicalMessage technicalMessage) {
        return new ErrorResponse(technicalMessage.getCode(), technicalMessage.getMessage());
    }
}
