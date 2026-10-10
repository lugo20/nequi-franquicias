package co.com.franquicias.usecase.createfranchise;

import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static co.com.franquicias.usecase.NameValidator.validName;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> execute(String name) {
        return validName(name)
                .map(validName -> new Franchise(UUID.randomUUID().toString(), validName, List.of()))
                .flatMap(franchiseRepository::saveFranchise);
    }
}
