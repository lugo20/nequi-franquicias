package co.com.franquicias.api.franchise;

import co.com.franquicias.api.dto.IdResponse;
import co.com.franquicias.api.dto.NameRequest;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {

    private final CreateFranchiseUseCase createFranchiseUseCase;

    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return request.bodyToMono(NameRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> createFranchiseUseCase.execute(body.name()))
                .flatMap(franchise -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(new IdResponse(franchise.id())));
    }
}
