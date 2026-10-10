package co.com.franquicias.usecase.updatefranchisename;

import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.NameValidator.validName;

@RequiredArgsConstructor
public class UpdateFranchiseNameUseCase {

    private final FranchiseRepository franchiseRepository;

    /** The name is unique system-wide: the repository rejects it atomically if another franchise uses it. */
    public Mono<Franchise> execute(String franchiseId, String name) {
        return validId(franchiseId).zipWith(validName(name))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> franchise.name().equals(input.getT2())
                                ? Mono.just(franchise)
                                : franchiseRepository.renameFranchise(franchise, input.getT2())));
    }
}
