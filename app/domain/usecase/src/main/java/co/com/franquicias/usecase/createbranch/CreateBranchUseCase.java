package co.com.franquicias.usecase.createbranch;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.NameValidator.isAvailable;
import static co.com.franquicias.usecase.NameValidator.validName;

@RequiredArgsConstructor
public class CreateBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Branch> execute(String franchiseId, String name) {
        return validId(franchiseId).zipWith(validName(name))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .filter(franchise -> isAvailable(franchise.branches().stream().map(Branch::name), input.getT2()))
                        .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NAME_DUPLICATED)))
                        .map(franchise -> new Branch(UUID.randomUUID().toString(), input.getT2(), List.of()))
                        .flatMap(branch -> franchiseRepository.saveBranch(input.getT1(), branch)));
    }
}
