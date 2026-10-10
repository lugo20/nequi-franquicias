package co.com.franquicias.usecase.deleteproduct;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductUseCaseTest {

    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of(
            new Branch("b1", "Norte", List.of(new Product("p1", "Cafe", 10))),
            new Branch("b2", "Sur", List.of(new Product("p3", "Pastel", 7)))));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private DeleteProductUseCase useCase;

    @Test
    void deletesExistingProduct() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.deleteProduct("f1", "b1", "p1")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f1", "b1", "p1")).verifyComplete();

        verify(franchiseRepository).deleteProduct("f1", "b1", "p1");
    }

    @Test
    void rejectsProductOfAnotherBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "p3"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.PRODUCT_NOT_FOUND))
                .verify();

        verify(franchiseRepository, never()).deleteProduct(anyString(), anyString(), anyString());
    }

    @Test
    void rejectsUnknownBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b9", "p1"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.BRANCH_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsUnknownFranchise() {
        when(franchiseRepository.findById("f9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f9", "b1", "p1"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.FRANCHISE_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsMissingProductId() {
        StepVerifier.create(useCase.execute("f1", "b1", null))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_REQUEST))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
