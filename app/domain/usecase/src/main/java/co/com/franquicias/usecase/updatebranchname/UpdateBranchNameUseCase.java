package co.com.franquicias.usecase.updatebranchname;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.franquicias.usecase.FranchiseLookup.requireBranch;
import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.NameValidator.isAvailable;
import static co.com.franquicias.usecase.NameValidator.validName;

@RequiredArgsConstructor
public class UpdateBranchNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Branch> execute(String franchiseId, String branchId, String name) {
        return Mono.zip(validId(franchiseId), validId(branchId), validName(name))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> requireBranch(franchise, input.getT2())
                                // Unique among the other branches; the branch may keep its own name with another case.
                                .filter(branch -> isAvailable(franchise.branches().stream()
                                        .filter(other -> !other.id().equals(branch.id()))
                                        .map(Branch::name), input.getT3()))
                                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NAME_DUPLICATED))))
                        .map(branch -> new Branch(branch.id(), input.getT3(), branch.products()))
                        .flatMap(branch -> franchiseRepository.saveBranch(input.getT1(), branch)));
    }
}
