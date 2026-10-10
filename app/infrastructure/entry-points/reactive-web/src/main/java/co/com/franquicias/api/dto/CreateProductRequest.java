package co.com.franquicias.api.dto;

public record CreateProductRequest(String franchiseId, String branchId, String name, Integer stock) {
}
