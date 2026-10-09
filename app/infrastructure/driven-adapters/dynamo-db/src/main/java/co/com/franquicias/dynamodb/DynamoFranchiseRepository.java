package co.com.franquicias.dynamodb;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import co.com.franquicias.model.exceptions.TechnicalException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import co.com.franquicias.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static co.com.franquicias.dynamodb.entity.FranchiseItem.BRANCH;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.FRANCHISE;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.PRODUCT;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.branchSk;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.franchisePk;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.productSk;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Repository
@RequiredArgsConstructor
public class DynamoFranchiseRepository implements FranchiseRepository {

    private final DynamoDbAsyncTable<FranchiseItem> table;

    @Override
    public Mono<Franchise> findById(String franchiseId) {
        QueryConditional byFranchise = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(franchisePk(franchiseId)).build());
        return Flux.defer(() -> Flux.from(table.query(byFranchise).items()))
                .collectList()
                .onErrorMap(TechnicalException::new)
                .flatMap(items -> Mono.justOrEmpty(toFranchise(items)));
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Mono<Void> deleteProduct(String franchiseId, String branchId, String productId) {
        Key key = Key.builder()
                .partitionValue(franchisePk(franchiseId))
                .sortValue(productSk(branchId, productId))
                .build();
        return Mono.fromFuture(() -> table.deleteItem(key))
                .onErrorMap(TechnicalException::new)
                .then();
    }

    private Mono<Void> put(FranchiseItem item) {
        return Mono.fromFuture(() -> table.putItem(item))
                .onErrorMap(TechnicalException::new);
    }

    private static Optional<Franchise> toFranchise(List<FranchiseItem> items) {
        Map<String, List<Product>> productsByBranch = items.stream()
                .filter(item -> PRODUCT.equals(item.getType()))
                .collect(groupingBy(FranchiseItem::getBranchId,
                        mapping(item -> new Product(item.getId(), item.getName(), item.getStock()), toList())));
        List<Branch> branches = items.stream()
                .filter(item -> BRANCH.equals(item.getType()))
                .map(item -> new Branch(item.getId(), item.getName(),
                        productsByBranch.getOrDefault(item.getId(), List.of())))
                .toList();
        return items.stream()
                .filter(item -> FRANCHISE.equals(item.getType()))
                .findFirst()
                .map(item -> new Franchise(item.getId(), item.getName(), branches));
    }
}
