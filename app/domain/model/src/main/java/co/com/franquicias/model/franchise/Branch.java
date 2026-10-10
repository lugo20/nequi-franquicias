package co.com.franquicias.model.franchise;

import java.util.List;

public record Branch(String id, String name, List<Product> products) {

    public Branch {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
