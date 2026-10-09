package co.com.franquicias.dynamodb;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.model.exceptions.TechnicalException;
import co.com.franquicias.model.franchise.Branch;
import co.com.franquicias.model.franchise.Franchise;
import co.com.franquicias.model.franchise.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoFranchiseRepositoryTest {

    @Mock
    private DynamoDbAsyncTable<FranchiseItem> table;
    @Mock
    private DynamoDbEnhancedAsyncClient client;

    private DynamoFranchiseRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DynamoFranchiseRepository(table, client);
    }

    @Test
    void saveFranchiseWritesFranchiseAndReservesNameInOneTransaction() {
        givenTableMetadata();
        when(client.transactWriteItems(any(TransactWriteItemsEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        Franchise franchise = new Franchise("f1", "Cafe Express", null);

        StepVerifier.create(repository.saveFranchise(franchise))
                .expectNext(franchise)
                .verifyComplete();

        ArgumentCaptor<TransactWriteItemsEnhancedRequest> captor =
                ArgumentCaptor.forClass(TransactWriteItemsEnhancedRequest.class);
        verify(client).transactWriteItems(captor.capture());
        List<Put> puts = captor.getValue().transactWriteItems().stream().map(item -> item.put()).toList();
        assertEquals(2, puts.size());
        assertEquals("FRANCHISE#f1", puts.get(0).item().get("franchiseKey").s());
        assertEquals("FRANCHISE", puts.get(0).item().get("entityKey").s());
        assertNull(puts.get(0).conditionExpression());
        assertEquals("FRANCHISE_NAME#cafe express", puts.get(1).item().get("franchiseKey").s());
        assertEquals("f1", puts.get(1).item().get("id").s());
        assertEquals("attribute_not_exists(franchiseKey)", puts.get(1).conditionExpression());
    }

    @Test
    void saveFranchiseFailsWithDuplicatedNameWhenReservationExists() {
        givenTableMetadata();
        TransactionCanceledException canceled = TransactionCanceledException.builder()
                .cancellationReasons(CancellationReason.builder().code("None").build(),
                        CancellationReason.builder().code("ConditionalCheckFailed").build())
                .build();
        when(client.transactWriteItems(any(TransactWriteItemsEnhancedRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new CompletionException(canceled)));

        StepVerifier.create(repository.saveFranchise(new Franchise("f2", "cafe express", null)))
                .expectErrorSatisfies(error -> assertEquals(TechnicalMessage.FRANCHISE_NAME_DUPLICATED,
                        ((BusinessException) error).getTechnicalMessage()))
                .verify();
    }

    @Test
    void saveFranchiseMapsOtherErrorsToTechnicalException() {
        givenTableMetadata();
        when(client.transactWriteItems(any(TransactWriteItemsEnhancedRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dynamo down")));

        StepVerifier.create(repository.saveFranchise(new Franchise("f1", "Cafe", null)))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void saveBranchWritesBranchItem() {
        when(table.putItem(any(FranchiseItem.class))).thenReturn(CompletableFuture.completedFuture(null));
        Branch branch = new Branch("b1", "Sucursal", null);

        StepVerifier.create(repository.saveBranch("f1", branch))
                .expectNext(branch)
                .verifyComplete();

        FranchiseItem item = capturePut();
        assertEquals("FRANCHISE#f1", item.getFranchiseKey());
        assertEquals("BRANCH#b1", item.getEntityKey());
        assertEquals("BRANCH", item.getType());
        assertNull(item.getStock());
    }

    @Test
    void saveProductWritesProductItem() {
        when(table.putItem(any(FranchiseItem.class))).thenReturn(CompletableFuture.completedFuture(null));
        Product product = new Product("p1", "Producto", 7);

        StepVerifier.create(repository.saveProduct("f1", "b1", product))
                .expectNext(product)
                .verifyComplete();

        FranchiseItem item = capturePut();
        assertEquals("FRANCHISE#f1", item.getFranchiseKey());
        assertEquals("BRANCH#b1#PRODUCT#p1", item.getEntityKey());
        assertEquals("b1", item.getBranchId());
        assertEquals(7, item.getStock());
    }

    @Test
    void saveMapsDynamoErrorToTechnicalException() {
        when(table.putItem(any(FranchiseItem.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dynamo down")));

        StepVerifier.create(repository.saveBranch("f1", new Branch("b1", "Sucursal", null)))
                .expectErrorSatisfies(error -> {
                    assertEquals(TechnicalException.class, error.getClass());
                    assertEquals(TechnicalMessage.TECHNICAL_ERROR, ((TechnicalException) error).getTechnicalMessage());
                })
                .verify();
    }

    @Test
    void findByIdBuildsAggregateFromItems() {
        givenQueryReturns(List.of(
                item("BRANCH#b1", "BRANCH", "b1", null, "Norte", null),
                item("BRANCH#b1#PRODUCT#p1", "PRODUCT", "p1", "b1", "Cafe", 10),
                item("BRANCH#b1#PRODUCT#p2", "PRODUCT", "p2", "b1", "Te", 3),
                item("BRANCH#b2", "BRANCH", "b2", null, "Sur", null),
                item("FRANCHISE", "FRANCHISE", "f1", null, "Franquicia", null)));

        StepVerifier.create(repository.findById("f1"))
                .assertNext(franchise -> {
                    assertEquals("f1", franchise.id());
                    assertEquals("Franquicia", franchise.name());
                    assertEquals(2, franchise.branches().size());
                    Branch north = franchise.branches().get(0);
                    assertEquals("Norte", north.name());
                    assertEquals(List.of(new Product("p1", "Cafe", 10), new Product("p2", "Te", 3)), north.products());
                    assertEquals(List.of(), franchise.branches().get(1).products());
                })
                .verifyComplete();
    }

    @Test
    void findByIdIsEmptyWhenFranchiseDoesNotExist() {
        givenQueryReturns(List.of());

        StepVerifier.create(repository.findById("missing")).verifyComplete();
    }

    @Test
    void findByIdMapsDynamoErrorToTechnicalException() {
        when(table.query(any(QueryConditional.class))).thenThrow(new RuntimeException("dynamo down"));

        StepVerifier.create(repository.findById("f1"))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void deleteProductDeletesProductKey() {
        when(table.deleteItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(repository.deleteProduct("f1", "b1", "p1")).verifyComplete();

        ArgumentCaptor<Key> key = ArgumentCaptor.forClass(Key.class);
        verify(table).deleteItem(key.capture());
        assertEquals("FRANCHISE#f1", key.getValue().partitionKeyValue().s());
        assertEquals("BRANCH#b1#PRODUCT#p1", key.getValue().sortKeyValue().orElseThrow().s());
    }

    private void givenTableMetadata() {
        when(table.tableName()).thenReturn("franquicias");
        when(table.tableSchema()).thenReturn(TableSchema.fromBean(FranchiseItem.class));
    }

    private FranchiseItem capturePut() {
        ArgumentCaptor<FranchiseItem> captor = ArgumentCaptor.forClass(FranchiseItem.class);
        verify(table).putItem(captor.capture());
        return captor.getValue();
    }

    private void givenQueryReturns(List<FranchiseItem> items) {
        when(table.query(any(QueryConditional.class)))
                .thenReturn(PagePublisher.create(SdkPublisher.adapt(Flux.just(Page.builder(FranchiseItem.class).items(items).build()))));
    }

    private static FranchiseItem item(String entityKey, String type, String id, String branchId, String name, Integer stock) {
        return new FranchiseItem("FRANCHISE#f1", entityKey, type, id, branchId, name, stock);
    }
}
