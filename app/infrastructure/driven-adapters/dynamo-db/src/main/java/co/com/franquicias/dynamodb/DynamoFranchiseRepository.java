package co.com.franquicias.dynamodb;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static co.com.franquicias.dynamodb.entity.FranchiseItem.BRANCH;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.FRANCHISE;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.FRANCHISE_NAME;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.PRODUCT;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.branchEntityKey;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.franchiseNameKey;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.franchiseKey;
import static co.com.franquicias.dynamodb.entity.FranchiseItem.productEntityKey;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Repository
@RequiredArgsConstructor
public class DynamoFranchiseRepository implements FranchiseRepository {

    private static final Expression NOT_EXISTS = Expression.builder().expression("attribute_not_exists(franchiseKey)").build();
    private static final String CONDITION_FAILED = "ConditionalCheckFailed";

    private final DynamoDbAsyncTable<FranchiseItem> table;
    private final DynamoDbEnhancedAsyncClient client;

    @Override
    public Mono<Franchise> findById(String franchiseId) {
        QueryConditional byFranchise = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(franchiseKey(franchiseId)).build());
        return Flux.defer(() -> Flux.from(table.query(byFranchise).items()))
                .collectList()
                .onErrorMap(TechnicalException::new)
                .flatMap(items -> Mono.justOrEmpty(toFranchise(items)));
    }

    /** Writes the franchise and reserves its name atomically; fails if the name is already taken. */
    @Override
    public Mono<Franchise> saveFranchise(Franchise franchise) {
        TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, franchiseItem(franchise.id(), franchise.name()))
                .addPutItem(table, reserveName(franchise.id(), franchise.name()))
                .build();
        return Mono.fromFuture(() -> client.transactWriteItems(request))
                .onErrorMap(error -> toNameError(error, 1))
                .thenReturn(franchise);
    }

    /**
     * Updates the name and moves the reservation in one transaction: the old reservation is released only if it
     * belongs to this franchise, and the new one is taken only if it is free.
     */
    @Override
    public Mono<Franchise> renameFranchise(Franchise franchise, String newName) {
        String oldNameKey = franchiseNameKey(franchise.name());
        String newNameKey = franchiseNameKey(newName);
        TransactWriteItemsEnhancedRequest.Builder request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, franchiseItem(franchise.id(), newName));
        if (oldNameKey.equals(newNameKey)) {
            // Only the letter case changes: same reservation, just refresh its stored name.
            request.addPutItem(table, TransactPutItemEnhancedRequest.builder(FranchiseItem.class)
                    .item(nameReservation(franchise.id(), newName))
                    .conditionExpression(ownedBy(franchise.id()))
                    .build());
        } else {
            request.addDeleteItem(table, TransactDeleteItemEnhancedRequest.builder()
                            .key(Key.builder().partitionValue(oldNameKey).sortValue(FRANCHISE_NAME).build())
                            .conditionExpression(ownedBy(franchise.id()))
                            .build())
                    .addPutItem(table, reserveName(franchise.id(), newName));
        }
        TransactWriteItemsEnhancedRequest built = request.build();
        int newReservationIndex = oldNameKey.equals(newNameKey) ? -1 : 2;
        return Mono.fromFuture(() -> client.transactWriteItems(built))
                .onErrorMap(error -> toNameError(error, newReservationIndex))
                .thenReturn(new Franchise(franchise.id(), newName, franchise.branches()));
    }

    @Override
    public Mono<Branch> saveBranch(String franchiseId, Branch branch) {
        return put(FranchiseItem.builder()
                .franchiseKey(franchiseKey(franchiseId))
                .entityKey(branchEntityKey(branch.id()))
                .type(BRANCH)
                .id(branch.id())
                .name(branch.name())
                .build())
                .thenReturn(branch);
    }

    @Override
    public Mono<Product> saveProduct(String franchiseId, String branchId, Product product) {
        return put(FranchiseItem.builder()
                .franchiseKey(franchiseKey(franchiseId))
                .entityKey(productEntityKey(branchId, product.id()))
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
                .partitionValue(franchiseKey(franchiseId))
                .sortValue(productEntityKey(branchId, productId))
                .build();
        return Mono.fromFuture(() -> table.deleteItem(key))
                .onErrorMap(TechnicalException::new)
                .then();
    }

    private Mono<Void> put(FranchiseItem item) {
        return Mono.fromFuture(() -> table.putItem(item))
                .onErrorMap(TechnicalException::new);
    }

    private static FranchiseItem franchiseItem(String franchiseId, String name) {
        return FranchiseItem.builder()
                .franchiseKey(franchiseKey(franchiseId))
                .entityKey(FRANCHISE)
                .type(FRANCHISE)
                .id(franchiseId)
                .name(name)
                .build();
    }

    private static FranchiseItem nameReservation(String franchiseId, String name) {
        return FranchiseItem.builder()
                .franchiseKey(franchiseNameKey(name))
                .entityKey(FRANCHISE_NAME)
                .type(FRANCHISE_NAME)
                .id(franchiseId)
                .name(name)
                .build();
    }

    private static TransactPutItemEnhancedRequest<FranchiseItem> reserveName(String franchiseId, String name) {
        return TransactPutItemEnhancedRequest.builder(FranchiseItem.class)
                .item(nameReservation(franchiseId, name))
                .conditionExpression(NOT_EXISTS)
                .build();
    }

    private static Expression ownedBy(String franchiseId) {
        return Expression.builder()
                .expression("#id = :id")
                .putExpressionName("#id", "id")
                .putExpressionValue(":id", AttributeValue.fromS(franchiseId))
                .build();
    }

    /** Name taken only when the condition of the new reservation (its position in the transaction) failed. */
    private static Throwable toNameError(Throwable error, int reservationIndex) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        boolean nameTaken = cause instanceof TransactionCanceledException canceled
                && reservationIndex >= 0
                && canceled.cancellationReasons().size() > reservationIndex
                && CONDITION_FAILED.equals(canceled.cancellationReasons().get(reservationIndex).code());
        return nameTaken
                ? new BusinessException(TechnicalMessage.FRANCHISE_NAME_DUPLICATED)
                : new TechnicalException(error);
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
