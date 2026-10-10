package co.com.franquicias.api.dto;

public record UpdateProductNameRequest(String franchiseId, String branchId, String productId, String name) {
}
