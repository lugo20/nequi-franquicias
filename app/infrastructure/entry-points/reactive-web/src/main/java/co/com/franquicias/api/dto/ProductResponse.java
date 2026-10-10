package co.com.franquicias.api.dto;

import co.com.franquicias.model.franchise.Product;

public record ProductResponse(String id, String name, Integer stock) {

    public static ProductResponse of(Product product) {
        return new ProductResponse(product.id(), product.name(), product.stock());
    }
}
