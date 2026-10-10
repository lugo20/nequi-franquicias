package co.com.franquicias.api;

import co.com.franquicias.api.branch.BranchHandler;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.usecase.createbranch.CreateBranchUseCase;
import co.com.franquicias.usecase.updatebranchname.UpdateBranchNameUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BranchRouterTest {

    private CreateBranchUseCase createBranchUseCase;
    private UpdateBranchNameUseCase updateBranchNameUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        createBranchUseCase = mock(CreateBranchUseCase.class);
        updateBranchNameUseCase = mock(UpdateBranchNameUseCase.class);
        client = RouterTestSupport.client(new RouterRest().branchRoutes(
                new BranchHandler(createBranchUseCase, updateBranchNameUseCase)));
    }

    @Test
    void createBranchReturns201WithId() {
        when(createBranchUseCase.execute("f1", "Norte")).thenReturn(Mono.just(new Branch("b1", "Norte", List.of())));

        post("{\"franchiseId\":\"f1\",\"name\":\"Norte\"}")
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isEqualTo("b1")
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void createBranchReturns404WhenFranchiseDoesNotExist() {
        when(createBranchUseCase.execute(any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.FRANCHISE_NOT_FOUND)));

        post("{\"franchiseId\":\"f9\",\"name\":\"Norte\"}")
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
    }

    @Test
    void createBranchReturns409WhenNameIsDuplicated() {
        when(createBranchUseCase.execute(any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NAME_DUPLICATED)));

        post("{\"franchiseId\":\"f1\",\"name\":\"norte\"}")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("BRANCH_NAME_DUPLICATED");
    }

    @Test
    void createBranchReturns400WhenBodyIsEmpty() {
        client.post().uri("/api/v1/branches/create")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void updateNameReturns200WithIdAndName() {
        when(updateBranchNameUseCase.execute("f1", "b1", "Norte Plaza"))
                .thenReturn(Mono.just(new Branch("b1", "Norte Plaza", List.of())));

        client.post().uri("/api/v1/branches/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"franchiseId\":\"f1\",\"branchId\":\"b1\",\"name\":\"Norte Plaza\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("b1")
                .jsonPath("$.name").isEqualTo("Norte Plaza")
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void updateNameReturns404WhenBranchDoesNotExist() {
        when(updateBranchNameUseCase.execute(any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NOT_FOUND)));

        client.post().uri("/api/v1/branches/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"franchiseId\":\"f1\",\"branchId\":\"b9\",\"name\":\"X\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND");
    }

    private WebTestClient.ResponseSpec post(String body) {
        return client.post().uri("/api/v1/branches/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }
}
