package co.com.franquicias.usecase.updatefranchisename;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Franchise;
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
class UpdateFranchiseNameUseCaseTest {

    private static final Franchise FRANCHISE = new Franchise("f1", "Cafe Express", List.of());

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateFranchiseNameUseCase useCase;

    @Test
    void renamesWithTrimmedName() {
        Franchise renamed = new Franchise("f1", "Cafe Premium", List.of());
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.renameFranchise(FRANCHISE, "Cafe Premium")).thenReturn(Mono.just(renamed));

        StepVerifier.create(useCase.execute("f1", "  Cafe Premium "))
                .expectNext(renamed)
                .verifyComplete();
    }

    @Test
    void doesNothingWhenTheNameIsTheSame() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "Cafe Express"))
                .expectNext(FRANCHISE)
                .verifyComplete();

        verify(franchiseRepository, never()).renameFranchise(any(), anyString());
    }

    @Test
    void propagatesDuplicatedName() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.renameFranchise(FRANCHISE, "Burger Town"))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.FRANCHISE_NAME_DUPLICATED)));

        StepVerifier.create(useCase.execute("f1", "Burger Town"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.FRANCHISE_NAME_DUPLICATED))
                .verify();
    }

    @Test
    void rejectsUnknownFranchise() {
        when(franchiseRepository.findById("f9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f9", "Otro"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.FRANCHISE_NOT_FOUND))
                .verify();
    }

    @Test
    void rejectsMissingName() {
        StepVerifier.create(useCase.execute("f1", null))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_NAME))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
