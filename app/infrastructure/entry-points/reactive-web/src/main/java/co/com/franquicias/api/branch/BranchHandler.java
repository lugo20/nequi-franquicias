package co.com.franquicias.api.branch;

import co.com.franquicias.api.dto.CreateBranchRequest;
import co.com.franquicias.api.dto.IdResponse;
import co.com.franquicias.api.dto.NamedResponse;
import co.com.franquicias.api.dto.UpdateBranchNameRequest;
import co.com.franquicias.model.enums.TechnicalMessage;
import co.com.franquicias.model.exceptions.BusinessException;
import co.com.franquicias.usecase.createbranch.CreateBranchUseCase;
import co.com.franquicias.usecase.updatebranchname.UpdateBranchNameUseCase;
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
    private final UpdateBranchNameUseCase updateBranchNameUseCase;

    public Mono<ServerResponse> createBranch(ServerRequest request) {
        return request.bodyToMono(CreateBranchRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> createBranchUseCase.execute(body.franchiseId(), body.name()))
                .flatMap(branch -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(new IdResponse(branch.id())));
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        return request.bodyToMono(UpdateBranchNameRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> updateBranchNameUseCase.execute(body.franchiseId(), body.branchId(), body.name()))
                .flatMap(branch -> ServerResponse.ok().bodyValue(new NamedResponse(branch.id(), branch.name())));
    }
}
