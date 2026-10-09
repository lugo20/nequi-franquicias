package co.com.franquicias.model.franchise;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FranchiseTest {

    @Test
    void nullListsBecomeEmpty() {
        assertTrue(new Franchise("f1", "Franquicia", null).branches().isEmpty());
        assertTrue(new Branch("b1", "Sucursal", null).products().isEmpty());
    }

    @Test
    void listsAreImmutableCopies() {
        List<Product> products = new ArrayList<>(List.of(new Product("p1", "Producto", 5)));
        Branch branch = new Branch("b1", "Sucursal", products);

        products.clear();

        assertEquals(1, branch.products().size());
        assertThrows(UnsupportedOperationException.class, () -> branch.products().clear());
    }
}
