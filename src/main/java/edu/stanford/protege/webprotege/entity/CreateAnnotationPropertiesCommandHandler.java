package edu.stanford.protege.webprotege.entity;

import edu.stanford.protege.webprotege.api.ActionExecutor;
import edu.stanford.protege.webprotege.ipc.CommandHandler;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.WebProtegeHandler;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-08-20
 */
@WebProtegeHandler
public class CreateAnnotationPropertiesCommandHandler implements CommandHandler<CreateAnnotationPropertiesAction, CreateAnnotationPropertiesResult> {

    private final ActionExecutor executor;

    public CreateAnnotationPropertiesCommandHandler(ActionExecutor executor) {
        this.executor = executor;
    }

    @NotNull
    @Override
    public String getChannelName() {
        return CreateAnnotationPropertiesAction.CHANNEL;
    }

    @Override
    public Class<CreateAnnotationPropertiesAction> getRequestClass() {
        return CreateAnnotationPropertiesAction.class;
    }

    @Override
    public Mono<CreateAnnotationPropertiesResult> handleRequest(CreateAnnotationPropertiesAction request,
                                                                ExecutionContext executionContext) {
        return Mono.just(executor.execute(request, executionContext));
    }
}