package co.com.franquicias.usecase.gettopstockproducts;

import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.TopStockProduct;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.Optional;

import static co.com.franquicias.usecase.FranchiseLookup.requireFranchise;
import static co.com.franquicias.usecase.FranchiseLookup.validId;

@RequiredArgsConstructor
public class GetTopStockProductsUseCase {

    // Highest stock first; on a tie, the first product by name.
    private static final Comparator<Product> TOP_STOCK = Comparator.comparing(Product::stock).reversed()
            .thenComparing(Product::name, String.CASE_INSENSITIVE_ORDER);

    private final FranchiseRepository franchiseRepository;

    /** One product per branch, ordered by branch name; branches without products are skipped. */
    public Flux<TopStockProduct> execute(String franchiseId) {
        return validId(franchiseId)
                .flatMap(id -> requireFranchise(franchiseRepository, id))
                .flatMapIterable(franchise -> franchise.branches().stream()
                        .sorted(Comparator.comparing(Branch::name, String.CASE_INSENSITIVE_ORDER))
                        .map(GetTopStockProductsUseCase::topOf)
                        .flatMap(Optional::stream)
                        .toList());
    }

    private static Optional<TopStockProduct> topOf(Branch branch) {
        return branch.products().stream()
                .min(TOP_STOCK)
                .map(product -> new TopStockProduct(branch.id(), branch.name(), product.id(), product.name(),
                        product.stock()));
    }
}
