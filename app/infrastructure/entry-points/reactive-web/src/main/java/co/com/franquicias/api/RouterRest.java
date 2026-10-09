package co.com.franquicias.api;

import co.com.franquicias.api.franchise.FranchiseHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private static final String FRANCHISES = "/api/v1/franchises";

    @Bean
    public RouterFunction<ServerResponse> routerFunction(FranchiseHandler franchiseHandler) {
        return route()
                .POST(FRANCHISES, franchiseHandler::createFranchise)
                .build();
    }
}
