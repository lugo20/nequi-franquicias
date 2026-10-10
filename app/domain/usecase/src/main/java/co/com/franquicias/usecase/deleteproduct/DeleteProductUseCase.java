package co.com.franquicias.usecase.deleteproduct;

import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.franquicias.usecase.FranchiseLookup.requireBranch;
import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.requireProduct;
import static co.com.franquicias.usecase.FranchiseLookup.validId;

@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Void> execute(String franchiseId, String branchId, String productId) {
        return Mono.zip(validId(franchiseId), validId(branchId), validId(productId))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> requireBranch(franchise, input.getT2()))
                        .flatMap(branch -> requireProduct(branch, input.getT3()))
                        .flatMap(product -> franchiseRepository.deleteProduct(input.getT1(), input.getT2(), product.id())));
    }
}
