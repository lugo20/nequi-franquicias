package co.com.franquicias.api.error;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Translates every error into an HTTP status with a {code, message} body.
 * Runs before Spring's default handler (order -1).
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        if (error instanceof BaseException baseException) {
            TechnicalMessage message = baseException.getTechnicalMessage();
            HttpStatus status = statusOf(message);
            if (status.is5xxServerError()) {
                log.error("Technical error", error);
            }
            return write(exchange.getResponse(), status, message);
        }
        if (error instanceof ServerWebInputException) {
            return write(exchange.getResponse(), HttpStatus.BAD_REQUEST, TechnicalMessage.INVALID_REQUEST);
        }
        // Routing errors (404 unknown path, 405 method) keep Spring's default handling.
        if (error instanceof ResponseStatusException) {
            return Mono.error(error);
        }
        log.error("Unexpected error", error);
        return write(exchange.getResponse(), HttpStatus.INTERNAL_SERVER_ERROR, TechnicalMessage.TECHNICAL_ERROR);
    }

    static HttpStatus statusOf(TechnicalMessage message) {
        return switch (message) {
            case FRANCHISE_NOT_FOUND, BRANCH_NOT_FOUND, PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FRANCHISE_NAME_DUPLICATED, BRANCH_NAME_DUPLICATED, PRODUCT_NAME_DUPLICATED -> HttpStatus.CONFLICT;
            case INVALID_NAME, INVALID_STOCK, INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case TECHNICAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private Mono<Void> write(ServerHttpResponse response, HttpStatus status, TechnicalMessage message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(ErrorResponse.of(message));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
