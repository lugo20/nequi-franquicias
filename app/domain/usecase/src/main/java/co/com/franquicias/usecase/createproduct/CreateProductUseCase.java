package co.com.franquicias.usecase.createproduct;

import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static co.com.franquicias.usecase.FranchiseLookup.requireBranch;
import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.validId;
import static co.com.franquicias.usecase.NameValidator.isAvailable;
import static co.com.franquicias.usecase.NameValidator.validName;
import static co.com.franquicias.usecase.StockValidator.validStock;

@RequiredArgsConstructor
public class CreateProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Product> execute(String franchiseId, String branchId, String name, Integer stock) {
        return Mono.zip(validId(franchiseId), validId(branchId), validName(name), validStock(stock))
                .flatMap(input -> requireFranchise(franchiseRepository, input.getT1())
                        .flatMap(franchise -> requireBranch(franchise, input.getT2()))
                        .filter(branch -> isAvailable(branch.products().stream().map(Product::name), input.getT3()))
                        .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.PRODUCT_NAME_DUPLICATED)))
                        .map(branch -> new Product(UUID.randomUUID().toString(), input.getT3(), input.getT4()))
                        .flatMap(product -> franchiseRepository.saveProduct(input.getT1(), input.getT2(), product)));
    }
}
