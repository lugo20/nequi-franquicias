package co.com.franquicias.api;

import co.com.franquicias.api.branch.BranchHandler;
import co.com.franquicias.api.franchise.FranchiseHandler;
import co.com.franquicias.api.product.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** One router per resource; Spring combines them. */
@Configuration
public class RouterRest {

    private static final String FRANCHISES = "/api/v1/franchises";
    private static final String BRANCHES = "/api/v1/branches";
    private static final String PRODUCTS = "/api/v1/products";
    private static final String CREATE = "/create";
    private static final String PRODUCT_PATH = "/{franchiseId}/{branchId}/{productId}";

    @Bean
    public RouterFunction<ServerResponse> franchiseRoutes(FranchiseHandler franchiseHandler) {
        return route()
                .POST(FRANCHISES + CREATE, franchiseHandler::createFranchise)
                .GET(FRANCHISES + "/{franchiseId}/get-top-stock", franchiseHandler::getTopStock)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> branchRoutes(BranchHandler branchHandler) {
        return route()
                .POST(BRANCHES + CREATE, branchHandler::createBranch)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler productHandler) {
        return route()
                .POST(PRODUCTS + CREATE, productHandler::createProduct)
                .POST(PRODUCTS + "/update-stock", productHandler::updateStock)
                .DELETE(PRODUCTS + PRODUCT_PATH + "/delete", productHandler::deleteProduct)
                .build();
    }
}
