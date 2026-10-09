package co.com.franquicias.dynamodb;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import co.com.franquicias.model.exceptions.TechnicalException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;

import static co.com.franquicias.dynamodb.entity.FranchiseItem.BRANCH;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.FRANCHISE;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.PRODUCT;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.branchSk;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.franchisePk;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.productSk;

@Repository
@RequiredArgsConstructor
public class DynamoFranchiseRepository {

    private final DynamoDbAsyncTable<FranchiseItem> table;

    public Mono<Franchise> saveFranchise(Franchise franchise) {
        return put(FranchiseItem.builder()
                .pk(franchisePk(franchise.id()))
                .sk(FRANCHISE)
                .type(FRANCHISE)
                .id(franchise.id())
                .name(franchise.name())
                .build())
                .thenReturn(franchise);
    }

    public Mono<Branch> saveBranch(String franchiseId, Branch branch) {
        return put(FranchiseItem.builder()
                .pk(franchisePk(franchiseId))
                .sk(branchSk(branch.id()))
                .type(BRANCH)
                .id(branch.id())
                .name(branch.name())
                .build())
                .thenReturn(branch);
    }

    public Mono<Product> saveProduct(String franchiseId, String branchId, Product product) {
        return put(FranchiseItem.builder()
                .pk(franchisePk(franchiseId))
                .sk(productSk(branchId, product.id()))
                .type(PRODUCT)
                .id(product.id())
                .branchId(branchId)
                .name(product.name())
                .stock(product.stock())
                .build())
                .thenReturn(product);
    }

    private Mono<Void> put(FranchiseItem item) {
        return Mono.fromFuture(() -> table.putItem(item))
                .onErrorMap(TechnicalException::new);
    }
}
