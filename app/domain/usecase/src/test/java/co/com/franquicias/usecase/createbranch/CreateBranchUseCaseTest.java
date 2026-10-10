package co.com.franquicias.usecase.createbranch;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBranchUseCaseTest {

    private static final Franchise FRANCHISE =
            new Franchise("f1", "Cafe Express", List.of(new Branch("b1", "Norte", List.of())));

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private CreateBranchUseCase useCase;

    @Test
    void createsBranchWithTrimmedNameAndNewId() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));
        when(franchiseRepository.saveBranch(eq("f1"), any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));

        StepVerifier.create(useCase.execute("f1", "  Sur  "))
                .assertNext(branch -> {
                    assertEquals("Sur", branch.name());
                    assertDoesNotThrow(() -> UUID.fromString(branch.id()));
                    assertTrue(branch.products().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void rejectsNameAlreadyUsedInTheFranchiseIgnoringCase() {
        when(franchiseRepository.findById("f1")).thenReturn(Mono.just(FRANCHISE));

        StepVerifier.create(useCase.execute("f1", "NORTE"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.BRANCH_NAME_DUPLICATED))
                .verify();

        verify(franchiseRepository, never()).saveBranch(anyString(), any());
    }

    @Test
    void rejectsUnknownFranchise() {
        when(franchiseRepository.findById("f9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("f9", "Sur"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.FRANCHISE_NOT_FOUND))
                .verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void rejectsMissingFranchiseId(String franchiseId) {
        StepVerifier.create(useCase.execute(franchiseId, "Sur"))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_REQUEST))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingName() {
        StepVerifier.create(useCase.execute("f1", "  "))
                .expectErrorSatisfies(error -> assertMessage(error, TechnicalMessage.INVALID_NAME))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    private static void assertMessage(Throwable error, TechnicalMessage expected) {
        assertEquals(expected, ((BusinessException) error).getTechnicalMessage());
    }
}
