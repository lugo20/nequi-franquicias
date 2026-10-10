package co.com.franquicias.api;

import co.com.franquicias.api.product.ProductHandler;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.usecase.createproduct.CreateProductUseCase;
import co.com.franquicias.usecase.deleteproduct.DeleteProductUseCase;
import co.com.franquicias.usecase.updateproductstock.UpdateProductStockUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductRouterTest {

    private CreateProductUseCase createProductUseCase;
    private DeleteProductUseCase deleteProductUseCase;
    private UpdateProductStockUseCase updateProductStockUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        createProductUseCase = mock(CreateProductUseCase.class);
        deleteProductUseCase = mock(DeleteProductUseCase.class);
        updateProductStockUseCase = mock(UpdateProductStockUseCase.class);
        client = RouterTestSupport.client(new RouterRest().productRoutes(
                new ProductHandler(createProductUseCase, deleteProductUseCase, updateProductStockUseCase)));
    }

    @Test
    void createProductReturns201WithId() {
        when(createProductUseCase.execute("f1", "b1", "Cafe", 10)).thenReturn(Mono.just(new Product("p1", "Cafe", 10)));

        post("/api/v1/products/create", "{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"name\":\"Cafe\",\"stock\":10}")
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isEqualTo("p1")
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void createProductReturns400WhenStockIsInvalid() {
        when(createProductUseCase.execute(any(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.INVALID_STOCK)));

        post("/api/v1/products/create", "{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"name\":\"Cafe\",\"stock\":-1}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_STOCK");
    }

    @Test
    void createProductReturns400WhenStockIsNotANumber() {
        post("/api/v1/products/create", "{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"name\":\"Cafe\",\"stock\":\"diez\"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        verify(createProductUseCase, never()).execute(any(), any(), any(), any());
    }

    @Test
    void createProductReturns404WhenBranchDoesNotExist() {
        when(createProductUseCase.execute(any(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NOT_FOUND)));

        post("/api/v1/products/create", "{\"franchiseId\":\"f1\",\"branchId\":\"b9\",\"name\":\"Cafe\",\"stock\":1}")
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND");
    }

    @Test
    void deleteProductReturns204() {
        when(deleteProductUseCase.execute("f1", "b1", "p1")).thenReturn(Mono.empty());

        client.delete().uri("/api/v1/products/f1/b1/p1/delete")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void deleteProductReturns404WhenProductDoesNotExist() {
        when(deleteProductUseCase.execute("f1", "b1", "p9"))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.PRODUCT_NOT_FOUND)));

        client.delete().uri("/api/v1/products/f1/b1/p9/delete")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void updateStockReturns200WithUpdatedProduct() {
        when(updateProductStockUseCase.execute("f1", "b1", "p2", 20)).thenReturn(Mono.just(new Product("p2", "Te", 20)));

        post("/api/v1/products/update-stock", "{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"productId\":\"p2\",\"stock\":20}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("p2")
                .jsonPath("$.name").isEqualTo("Te")
                .jsonPath("$.stock").isEqualTo(20);
    }

    @Test
    void updateStockReturns400WhenStockIsNegative() {
        when(updateProductStockUseCase.execute(any(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.INVALID_STOCK)));

        post("/api/v1/products/update-stock", "{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"productId\":\"p2\",\"stock\":-1}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_STOCK");
    }

    private WebTestClient.ResponseSpec post(String uri, String body) {
        return client.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }
}
