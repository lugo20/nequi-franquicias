package co.com.franquicias.api;

import co.com.franquicias.api.error.GlobalErrorHandler;
import co.com.franquicias.api.franchise.FranchiseHandler;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FranchiseRouterTest {

    private CreateFranchiseUseCase createFranchiseUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        createFranchiseUseCase = mock(CreateFranchiseUseCase.class);
        FranchiseHandler handler = new FranchiseHandler(createFranchiseUseCase);
        client = WebTestClient
                .bindToRouterFunction(new RouterRest().routerFunction(handler))
                // empty() avoids Spring's default 400 handler so errors reach ours, as in the running app.
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(new GlobalErrorHandler(new ObjectMapper()))
                        .build())
                .build();
    }

    @Test
    void createFranchiseReturns201WithId() {
        when(createFranchiseUseCase.execute("Cafe Express"))
                .thenReturn(Mono.just(new Franchise("f1", "Cafe Express", List.of())));

        post("{\"name\":\"Cafe Express\"}")
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void createFranchiseReturns400WhenNameIsInvalid() {
        when(createFranchiseUseCase.execute(any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.INVALID_NAME)));

        post("{\"name\":\"  \"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_NAME");
    }

    @Test
    void createFranchiseReturns400WhenBodyIsMalformed() {
        post("{name:")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        verify(createFranchiseUseCase, never()).execute(any());
    }

    @Test
    void createFranchiseReturns400WhenBodyIsEmpty() {
        client.post().uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    private WebTestClient.ResponseSpec post(String body) {
        return client.post().uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }
}
