package co.com.franquicias.api.dto;

public record UpdateStockRequest(String franchiseId, String branchId, String productId, Integer stock) {
}
