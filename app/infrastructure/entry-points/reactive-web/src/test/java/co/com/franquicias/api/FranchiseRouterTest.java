package co.com.franquicias.api;

import co.com.franquicias.api.franchise.FranchiseHandler;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import co.com.franquicias.usecase.gettopstockproducts.GetTopStockProductsUseCase;
import co.com.franquicias.usecase.updatefranchisename.UpdateFranchiseNameUseCase;
import co.com.franquicias.model.franchise.TopStockProduct;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FranchiseRouterTest {

    private CreateFranchiseUseCase createFranchiseUseCase;
    private GetTopStockProductsUseCase getTopStockProductsUseCase;
    private UpdateFranchiseNameUseCase updateFranchiseNameUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        createFranchiseUseCase = mock(CreateFranchiseUseCase.class);
        getTopStockProductsUseCase = mock(GetTopStockProductsUseCase.class);
        updateFranchiseNameUseCase = mock(UpdateFranchiseNameUseCase.class);
        FranchiseHandler handler = new FranchiseHandler(createFranchiseUseCase, getTopStockProductsUseCase,
                updateFranchiseNameUseCase);
        client = RouterTestSupport.client(new RouterRest().franchiseRoutes(handler));
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
        client.post().uri("/api/v1/franchises/create")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void getTopStockReturns200WithOneProductPerBranch() {
        when(getTopStockProductsUseCase.execute("f1")).thenReturn(Flux.just(
                new TopStockProduct("b1", "Norte", "p1", "Cafe", 10),
                new TopStockProduct("b2", "Sur", "p3", "Pastel", 7)));

        client.get().uri("/api/v1/franchises/f1/get-top-stock")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].branchName").isEqualTo("Norte")
                .jsonPath("$[0].productName").isEqualTo("Cafe")
                .jsonPath("$[0].stock").isEqualTo(10)
                .jsonPath("$[1].branchId").isEqualTo("b2");
    }

    @Test
    void getTopStockReturnsEmptyListWhenThereAreNoProducts() {
        when(getTopStockProductsUseCase.execute("f1")).thenReturn(Flux.empty());

        client.get().uri("/api/v1/franchises/f1/get-top-stock")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void getTopStockReturns404WhenFranchiseDoesNotExist() {
        when(getTopStockProductsUseCase.execute("f9"))
                .thenReturn(Flux.error(new BusinessException(TechnicalMessage.FRANCHISE_NOT_FOUND)));

        client.get().uri("/api/v1/franchises/f9/get-top-stock")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
    }

    @Test
    void updateNameReturns200WithIdAndName() {
        when(updateFranchiseNameUseCase.execute("f1", "Cafe Premium"))
                .thenReturn(Mono.just(new Franchise("f1", "Cafe Premium", List.of())));

        client.post().uri("/api/v1/franchises/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"franchiseId\":\"f1\",\"name\":\"Cafe Premium\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.name").isEqualTo("Cafe Premium");
    }

    @Test
    void updateNameReturns409WhenNameIsTaken() {
        when(updateFranchiseNameUseCase.execute(any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.FRANCHISE_NAME_DUPLICATED)));

        client.post().uri("/api/v1/franchises/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"franchiseId\":\"f1\",\"name\":\"Burger Town\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("FRANCHISE_NAME_DUPLICATED");
    }

    private WebTestClient.ResponseSpec post(String body) {
        return client.post().uri("/api/v1/franchises/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }
}
