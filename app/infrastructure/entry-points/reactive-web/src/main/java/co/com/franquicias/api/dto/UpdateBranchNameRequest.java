package co.com.franquicias.api.dto;

public record UpdateBranchNameRequest(String franchiseId, String branchId, String name) {
}
