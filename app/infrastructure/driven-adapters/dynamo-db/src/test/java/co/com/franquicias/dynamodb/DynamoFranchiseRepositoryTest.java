package co.com.franquicias.dynamodb;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import co.com.franquicias.model.enums.TechnicalMessage;
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
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoFranchiseRepositoryTest {

    @Mock
    private DynamoDbAsyncTable<FranchiseItem> table;

    private DynamoFranchiseRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DynamoFranchiseRepository(table);
    }

    @Test
    void saveFranchiseWritesFranchiseItem() {
        when(table.putItem(any(FranchiseItem.class))).thenReturn(CompletableFuture.completedFuture(null));
        Franchise franchise = new Franchise("f1", "Franquicia", null);

        StepVerifier.create(repository.saveFranchise(franchise))
                .expectNext(franchise)
                .verifyComplete();

        FranchiseItem item = capturePut();
        assertEquals("FRANCHISE#f1", item.getPk());
        assertEquals("FRANCHISE", item.getSk());
        assertEquals("Franquicia", item.getName());
    }

    @Test
    void saveBranchWritesBranchItem() {
        when(table.putItem(any(FranchiseItem.class))).thenReturn(CompletableFuture.completedFuture(null));
        Branch branch = new Branch("b1", "Sucursal", null);

        StepVerifier.create(repository.saveBranch("f1", branch))
                .expectNext(branch)
                .verifyComplete();

        FranchiseItem item = capturePut();
        assertEquals("FRANCHISE#f1", item.getPk());
        assertEquals("BRANCH#b1", item.getSk());
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
        assertEquals("FRANCHISE#f1", item.getPk());
        assertEquals("BRANCH#b1#PRODUCT#p1", item.getSk());
        assertEquals("b1", item.getBranchId());
        assertEquals(7, item.getStock());
    }

    @Test
    void saveMapsDynamoErrorToTechnicalException() {
        when(table.putItem(any(FranchiseItem.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dynamo down")));

        StepVerifier.create(repository.saveFranchise(new Franchise("f1", "Franquicia", null)))
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

    private FranchiseItem capturePut() {
        ArgumentCaptor<FranchiseItem> captor = ArgumentCaptor.forClass(FranchiseItem.class);
        verify(table).putItem(captor.capture());
        return captor.getValue();
    }

    private void givenQueryReturns(List<FranchiseItem> items) {
        when(table.query(any(QueryConditional.class)))
                .thenReturn(PagePublisher.create(SdkPublisher.adapt(Flux.just(Page.builder(FranchiseItem.class).items(items).build()))));
    }

    private static FranchiseItem item(String sk, String type, String id, String branchId, String name, Integer stock) {
        return new FranchiseItem("FRANCHISE#f1", sk, type, id, branchId, name, stock);
    }
}
