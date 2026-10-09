package co.com.franquicias.api.error;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.exceptions.TechnicalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler(new ObjectMapper());

    @ParameterizedTest
    @EnumSource(TechnicalMessage.class)
    void businessErrorsReturnTheirStatusAndBody(TechnicalMessage message) {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new BusinessException(message))).verifyComplete();

        assertEquals(GlobalErrorHandler.statusOf(message), exchange.getResponse().getStatusCode());
        assertBody(exchange, message);
    }

    @Test
    void statusMapping() {
        assertEquals(HttpStatus.NOT_FOUND, GlobalErrorHandler.statusOf(TechnicalMessage.FRANCHISE_NOT_FOUND));
        assertEquals(HttpStatus.CONFLICT, GlobalErrorHandler.statusOf(TechnicalMessage.BRANCH_NAME_DUPLICATED));
        assertEquals(HttpStatus.BAD_REQUEST, GlobalErrorHandler.statusOf(TechnicalMessage.INVALID_STOCK));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, GlobalErrorHandler.statusOf(TechnicalMessage.TECHNICAL_ERROR));
    }

    @Test
    void technicalExceptionReturns500WithoutInternalDetails() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new TechnicalException(new RuntimeException("db password wrong"))))
                .verifyComplete();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertBody(exchange, TechnicalMessage.TECHNICAL_ERROR);
    }

    @Test
    void malformedBodyReturns400InvalidRequest() throws NoSuchMethodException {
        MockServerWebExchange exchange = exchange();
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);

        StepVerifier.create(handler.handle(exchange, new ServerWebInputException("bad json", parameter)))
                .verifyComplete();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertBody(exchange, TechnicalMessage.INVALID_REQUEST);
    }

    @Test
    void unexpectedErrorReturns500() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new IllegalStateException("boom"))).verifyComplete();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertBody(exchange, TechnicalMessage.TECHNICAL_ERROR);
    }

    @Test
    void routingErrorsAreLeftToSpring() {
        ResponseStatusException notFound = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange(), notFound))
                .expectErrorMatches(error -> error == notFound)
                .verify();
    }

    @Test
    void committedResponseIsNotRewritten() {
        MockServerWebExchange exchange = exchange();
        exchange.getResponse().setComplete().block();
        IllegalStateException error = new IllegalStateException("late");

        StepVerifier.create(handler.handle(exchange, error))
                .expectErrorMatches(e -> e == error)
                .verify();
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/franchises"));
    }

    private static void assertBody(MockServerWebExchange exchange, TechnicalMessage message) {
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertEquals("{\"code\":\"" + message.getCode() + "\",\"message\":\"" + message.getMessage() + "\"}", body);
    }
}
