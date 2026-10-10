package co.com.franquicias.usecase;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import reactor.core.publisher.Mono;

public final class FranchiseLookup {

    private FranchiseLookup() {
    }

    /** Emits the id, or INVALID_REQUEST when it is null or blank. */
    public static Mono<String> validId(String id) {
        return Mono.justOrEmpty(id)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)));
    }

    /** Emits the whole franchise, or FRANCHISE_NOT_FOUND. */
    public static Mono<Franchise> requireFranchise(FranchiseRepository repository, String franchiseId) {
        return repository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.FRANCHISE_NOT_FOUND)));
    }

    /** Emits the branch of the franchise, or BRANCH_NOT_FOUND. */
    public static Mono<Branch> requireBranch(Franchise franchise, String branchId) {
        return Mono.justOrEmpty(franchise.branches().stream()
                        .filter(branch -> branch.id().equals(branchId))
                        .findFirst())
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.BRANCH_NOT_FOUND)));
    }
}
