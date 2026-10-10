package co.com.franquicias.model.franchise.gateways;

import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {

    Mono<Franchise> findById(String franchiseId);

    Mono<Franchise> saveFranchise(Franchise franchise);

    /** Renames the franchise and moves its name reservation atomically. */
    Mono<Franchise> renameFranchise(Franchise franchise, String newName);

    Mono<Branch> saveBranch(String franchiseId, Branch branch);

    Mono<Product> saveProduct(String franchiseId, String branchId, Product product);

    Mono<Void> deleteProduct(String franchiseId, String branchId, String productId);
}
