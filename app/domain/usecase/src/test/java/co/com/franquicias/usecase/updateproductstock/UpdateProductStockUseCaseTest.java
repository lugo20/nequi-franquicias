package co.com.franquicias.usecase.updateproductstock;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductStockUseCaseTest {

    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of(
            new Branch("b1", "Norte", List.of(new Product("p2", "Te", 3)))));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateProductStockUseCase useCase;

    @Test
    void updatesOnlyTheStockAndKeepsTheName() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveProduct(eq("f1"), eq("b1"), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));

        StepVerifier.create(useCase.execute("f1", "b1", "p2", 20))
                .expectNext(new Product("p2", "Te", 20))
                .verifyComplete();
    }

    @Test
    void allowsZeroStock() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveProduct(eq("f1"), eq("b1"), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));

        StepVerifier.create(useCase.execute("f1", "b1", "p2", 0))
                .expectNext(new Product("p2", "Te", 0))
                .verifyComplete();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-5})
    void rejectsMissingOrNegativeStock(Integer stock) {
        StepVerifier.create(useCase.execute("f1", "b1", "p2", stock))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_STOCK))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    @Test
    void rejectsUnknownProduct() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "p9", 5))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.PRODUCT_NOT_FOUND))
                .verify();
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
