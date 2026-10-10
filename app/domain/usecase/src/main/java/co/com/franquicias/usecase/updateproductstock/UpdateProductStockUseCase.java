package co.com.franquicias.usecase.updateproductstock;

import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.franquicias.usecase.FranchiseLookup.requireBranch;
import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.requireProduct;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.StockValidator.validStock;

@RequiredArgsConstructor
public class UpdateProductStockUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Product> execute(String franchiseId, String branchId, String productId, Integer stock) {
        return Mono.zip(validId(franchiseId), validId(branchId), validId(productId), validStock(stock))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> requireBranch(franchise, input.getT2()))
                        .flatMap(branch -> requireProduct(branch, input.getT3()))
                        .map(product -> new Product(product.id(), product.name(), input.getT4()))
                        .flatMap(product -> franchiseRepository.saveProduct(input.getT1(), input.getT2(), product)));
    }
}
