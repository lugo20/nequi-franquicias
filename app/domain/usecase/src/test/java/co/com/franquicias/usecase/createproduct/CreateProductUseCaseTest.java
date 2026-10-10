package co.com.franquicias.usecase.createproduct;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of(
            new Branch("b1", "Norte", List.of(new Product("p1", "Cafe", 10))),
            new Branch("b2", "Sur", List.of())));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private CreateProductUseCase useCase;

    @Test
    void createsProductInTheBranchWithTrimmedName() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveProduct(eq("f1"), eq("b1"), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));

        StepVerifier.create(useCase.execute("f1", "b1", "  Te  ", 3))
                .assertNext(product -> {
                    assertEquals("Te", product.name());
                    assertEquals(3, product.stock());
                    assertDoesNotThrow(() -> UUID.fromString(product.id()));
                })
                .verifyComplete();
    }

    @Test
    void allowsSameNameInAnotherBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveProduct(eq("f1"), eq("b2"), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));

        StepVerifier.create(useCase.execute("f1", "b2", "Cafe", 0))
                .assertNext(product -> assertEquals("Cafe", product.name()))
                .verifyComplete();
    }

    @Test
    void rejectsNameAlreadyUsedInTheBranchIgnoringCase() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "CAFE", 5))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.PRODUCT_NAME_DUPLICATED))
                .verify();

        verify(franchiseRepository, never()).saveProduct(anyString(), anyString(), any());
    }

    @Test
    void rejectsUnknownBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b9", "Te", 3))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.BRANCH_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsUnknownFranchise() {
        when(franchiseRepository.findById("f9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f9", "b1", "Te", 3))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.FRANCHISE_NOT_FOUND))
                .verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1})
    void rejectsMissingOrNegativeStock(Integer stock) {
        StepVerifier.create(useCase.execute("f1", "b1", "Te", stock))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_STOCK))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingBranchId() {
        StepVerifier.create(useCase.execute("f1", " ", "Te", 3))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_REQUEST))
                .verify();
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
