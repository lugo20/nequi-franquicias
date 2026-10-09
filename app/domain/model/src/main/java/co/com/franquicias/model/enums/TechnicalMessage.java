package co.com.franquicias.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalMessage {
    FRANCHISE_NOT_FOUND("La franquicia no existe"),
    BRANCH_NOT_FOUND("La sucursal no existe"),
    PRODUCT_NOT_FOUND("El producto no existe"),
    BRANCH_NAME_DUPLICATED("Ya existe una sucursal con ese nombre en la franquicia"),
    PRODUCT_NAME_DUPLICATED("Ya existe un producto con ese nombre en la sucursal"),
    INVALID_NAME("El nombre es obligatorio"),
    INVALID_STOCK("El stock es obligatorio y debe ser mayor o igual a 0"),
    INVALID_REQUEST("El cuerpo de la petición es inválido"),
    TECHNICAL_ERROR("Ocurrió un error inesperado, intenta más tarde");

    private final String message;

    public String getCode() {
        return name();
    }
}
