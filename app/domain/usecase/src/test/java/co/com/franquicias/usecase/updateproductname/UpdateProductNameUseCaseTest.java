package co.com.franquicias.usecase.updateproductname;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductNameUseCaseTest {

    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of(
            new Branch("b1", "Norte", List.of(new Product("p1", "Cafe", 10), new Product("p2", "Te", 3)))));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateProductNameUseCase useCase;

    @Test
    void renamesAndKeepsTheStock() {
        givenFranchiseAndSave();

        StepVerifier.create(useCase.execute("f1", "b1", "p2", " Te Verde "))
                .expectNext(new Product("p2", "Te Verde", 3))
                .verifyComplete();
    }

    @Test
    void allowsChangingOnlyTheCaseOfItsOwnName() {
        givenFranchiseAndSave();

        StepVerifier.create(useCase.execute("f1", "b1", "p2", "TE"))
                .expectNext(new Product("p2", "TE", 3))
                .verifyComplete();
    }

    @Test
    void rejectsNameOfAnotherProductOfTheBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "p2", "cafe"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.PRODUCT_NAME_DUPLICATED))
                .verify();

        verify(franchiseRepository, never()).saveProduct(anyString(), anyString(), any());
    }

    @Test
    void rejectsUnknownProduct() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "p9", "Jugo"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.PRODUCT_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsMissingName() {
        StepVerifier.create(useCase.execute("f1", "b1", "p2", ""))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_NAME))
                .verify();
    }

    private void givenFranchiseAndSave() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveProduct(eq("f1"), eq("b1"), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
