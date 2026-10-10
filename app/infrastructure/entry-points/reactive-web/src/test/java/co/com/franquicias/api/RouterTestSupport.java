package co.com.franquicias.api;

import co.com.franquicias.api.error.GlobalErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

final class RouterTestSupport {

    private RouterTestSupport() {
    }

    /** Client over the given routes with the real error handler, as in the running app. */
    static WebTestClient client(RouterFunction<ServerResponse> routes) {
        return WebTestClient.bindToRouterFunction(routes)
                // empty() avoids Spring's default 400 handler so errors reach ours.
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(new GlobalErrorHandler(new ObjectMapper()))
                        .build())
                .build();
    }
}
