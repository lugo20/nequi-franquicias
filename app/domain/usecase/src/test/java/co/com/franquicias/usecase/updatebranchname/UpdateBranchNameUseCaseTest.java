package co.com.franquicias.usecase.updatebranchname;

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
class UpdateBranchNameUseCaseTest {

    private static final List<Product> PRODUCTS = List.of(new Product("p1", "Cafe", 10));
    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of(
            new Branch("b1", "Norte", PRODUCTS),
            new Branch("b2", "Sur", List.of())));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateBranchNameUseCase useCase;

    @Test
    void renamesAndKeepsTheProducts() {
        givenFranchiseAndSave();

        StepVerifier.create(useCase.execute("f1", "b1", " Norte Plaza "))
                .expectNext(new Branch("b1", "Norte Plaza", PRODUCTS))
                .verifyComplete();
    }

    @Test
    void allowsChangingOnlyTheCaseOfItsOwnName() {
        givenFranchiseAndSave();

        StepVerifier.create(useCase.execute("f1", "b1", "NORTE"))
                .expectNext(new Branch("b1", "NORTE", PRODUCTS))
                .verifyComplete();
    }

    @Test
    void rejectsNameOfAnotherBranchIgnoringCase() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b1", "sur"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.BRANCH_NAME_DUPLICATED))
                .verify();

        verify(franchiseRepository, never()).saveBranch(anyString(), any());
    }

    @Test
    void rejectsUnknownBranch() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "b9", "Centro"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.BRANCH_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsMissingBranchId() {
        StepVerifier.create(useCase.execute("f1", null, "Centro"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_REQUEST))
                .verify();
    }

    private void givenFranchiseAndSave() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveBranch(eq("f1"), any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
