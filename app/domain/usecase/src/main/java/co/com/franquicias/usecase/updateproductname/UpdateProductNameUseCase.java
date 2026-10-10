package co.com.franquicias.usecase.updateproductname;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.franquicias.usecase.FranchiseLookup.requireBranch;
import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.requireProduct;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.NameValidator.isAvailable;
import static co.com.franquicias.usecase.NameValidator.validName;

@RequiredArgsConstructor
public class UpdateProductNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Product> execute(String franchiseId, String branchId, String productId, String name) {
        return Mono.zip(validId(franchiseId), validId(branchId), validId(productId), validName(name))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> requireBranch(franchise, input.getT2()))
                        .flatMap(branch -> requireProduct(branch, input.getT3())
                                // Unique among the other products; the product may keep its own name with another case.
                                .filter(product -> isAvailable(branch.products().stream()
                                        .filter(other -> !other.id().equals(product.id()))
                                        .map(Product::name), input.getT4()))
                                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.PRODUCT_NAME_DUPLICATED))))
                        .map(product -> new Product(product.id(), input.getT4(), product.stock()))
                        .flatMap(product -> franchiseRepository.saveProduct(input.getT1(), input.getT2(), product)));
    }
}
