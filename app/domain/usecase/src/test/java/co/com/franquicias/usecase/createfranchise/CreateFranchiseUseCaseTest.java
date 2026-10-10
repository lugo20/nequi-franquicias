package co.com.franquicias.usecase.createfranchise;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private CreateFranchiseUseCase useCase;

    @Test
    void createsFranchiseWithTrimmedNameAndNewId() {
        when(franchiseRepository.saveFranchise(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute("  Cafe Express  "))
                .assertNext(franchise -> {
                    assertEquals("Cafe Express", franchise.name());
                    assertDoesNotThrow(() -> UUID.fromString(franchise.id()));
                    assertTrue(franchise.branches().isEmpty());
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsMissingName(String name) {
        StepVerifier.create(useCase.execute(name))
                .expectErrorSatisfies(error -> assertEquals(TechnicalMessage.INVALID_NAME,
                        ((BusinessException) error).getTechnicalMessage()))
                .verify();

        verify(franchiseRepository, never()).saveFranchise(any(Franchise.class));
    }
}
