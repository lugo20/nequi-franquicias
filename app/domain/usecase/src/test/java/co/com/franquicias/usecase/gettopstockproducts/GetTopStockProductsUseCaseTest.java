package co.com.franquicias.usecase.gettopstockproducts;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.TopStockProduct;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductsUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private GetTopStockProductsUseCase useCase;

    @Test
    void returnsTheTopProductOfEachBranchOrderedByBranchName() {
        givenFranchise(
                new Branch("b2", "Sur", List.of(new Product("p3", "Pastel", 7), new Product("p4", "Jugo", 2))),
                new Branch("b1", "Norte", List.of(new Product("p1", "Cafe", 10), new Product("p2", "Te", 3))));

        StepVerifier.create(useCase.execute("f1"))
                .expectNext(new TopStockProduct("b1", "Norte", "p1", "Cafe", 10))
                .expectNext(new TopStockProduct("b2", "Sur", "p3", "Pastel", 7))
                .verifyComplete();
    }

    @Test
    void onATieReturnsTheFirstProductByName() {
        givenFranchise(new Branch("b1", "Norte", List.of(
                new Product("p1", "te", 5), new Product("p2", "Cafe", 5), new Product("p3", "Agua", 1))));

        StepVerifier.create(useCase.execute("f1"))
                .expectNext(new TopStockProduct("b1", "Norte", "p2", "Cafe", 5))
                .verifyComplete();
    }

    @Test
    void skipsBranchesWithoutProducts() {
        givenFranchise(
                new Branch("b1", "Norte", List.of()),
                new Branch("b2", "Sur", List.of(new Product("p3", "Pastel", 0))));

        StepVerifier.create(useCase.execute("f1"))
                .expectNext(new TopStockProduct("b2", "Sur", "p3", "Pastel", 0))
                .verifyComplete();
    }

    @Test
    void isEmptyWhenTheFranchiseHasNoBranches() {
        givenFranchise();

        StepVerifier.create(useCase.execute("f1")).verifyComplete();
    }

    @Test
    void rejectsUnknownFranchise() {
        when(franchiseRepository.findById("f9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f9"))
                .expectErrorSatisfies(error -> assertEquals(TechnicalMessage.FRANCHISE_NOT_FOUND,
                        ((BusinessException) error).getTechnicalMessage()))
                .verify();
    }

    @Test
    void rejectsMissingFranchiseId() {
        StepVerifier.create(useCase.execute(" "))
                .expectError(BusinessException.class)
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    private void givenFranchise(Branch... branches) {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(new Franchise("f1", "Cafe Express", List.of(branches))));
    }
}
