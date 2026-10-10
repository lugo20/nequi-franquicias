package co.com.franquicias.usecase;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import reactor.core.publisher.Mono;

public final class StockValidator {

    private StockValidator() {
    }

    /** Emits the stock, or INVALID_STOCK when it is null or negative. */
    public static Mono<Integer> validStock(Integer stock) {
        return Mono.justOrEmpty(stock)
                .filter(value -> value >= 0)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_STOCK)));
    }
}
