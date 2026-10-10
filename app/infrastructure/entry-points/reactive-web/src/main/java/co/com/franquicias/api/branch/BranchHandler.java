package co.com.franquicias.api.branch;

import co.com.franquicias.api.dto.CreateBranchRequest;
import co.com.franquicias.api.dto.IdResponse;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.usecase.createbranch.CreateBranchUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {

    private final CreateBranchUseCase createBranchUseCase;

    public Mono<ServerResponse> createBranch(ServerRequest request) {
        return request.bodyToMono(CreateBranchRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> createBranchUseCase.execute(body.franchiseId(), body.name()))
                .flatMap(branch -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(new IdResponse(branch.id())));
    }
}
