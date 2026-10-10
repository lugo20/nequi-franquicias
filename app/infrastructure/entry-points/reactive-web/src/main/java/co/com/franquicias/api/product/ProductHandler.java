package co.com.franquicias.api.product;

import co.com.franquicias.api.dto.CreateProductRequest;
import co.com.franquicias.api.dto.IdResponse;
import co.com.franquicias.api.dto.ProductResponse;
import co.com.franquicias.api.dto.UpdateProductNameRequest;
import co.com.franquicias.api.dto.UpdateStockRequest;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.usecase.createproduct.CreateProductUseCase;
import co.com.franquicias.usecase.deleteproduct.DeleteProductUseCase;
import co.com.franquicias.usecase.updateproductname.UpdateProductNameUseCase;
import co.com.franquicias.usecase.updateproductstock.UpdateProductStockUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductHandler {

    private final CreateProductUseCase createProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final UpdateProductNameUseCase updateProductNameUseCase;

    public Mono<ServerResponse> createProduct(ServerRequest request) {
        return request.bodyToMono(CreateProductRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> createProductUseCase.execute(body.franchiseId(), body.branchId(), body.name(), body.stock()))
                .flatMap(product -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(new IdResponse(product.id())));
    }

    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        return deleteProductUseCase.execute(request.pathVariable("franchiseId"), request.pathVariable("branchId"),
                        request.pathVariable("productId"))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateStock(ServerRequest request) {
        return request.bodyToMono(UpdateStockRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> updateProductStockUseCase.execute(body.franchiseId(), body.branchId(),
                        body.productId(), body.stock()))
                .flatMap(product -> ServerResponse.ok().bodyValue(ProductResponse.of(product)));
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        return request.bodyToMono(UpdateProductNameRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> updateProductNameUseCase.execute(body.franchiseId(), body.branchId(),
                        body.productId(), body.name()))
                .flatMap(product -> ServerResponse.ok().bodyValue(ProductResponse.of(product)));
    }
}
