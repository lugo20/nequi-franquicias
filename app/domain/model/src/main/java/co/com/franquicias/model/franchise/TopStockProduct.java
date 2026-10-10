package co.com.franquicias.model.franchise;

/** Product with the highest stock of a branch. */
public record TopStockProduct(String branchId, String branchName, String productId, String productName, Integer stock) {
}
