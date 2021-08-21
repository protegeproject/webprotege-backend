package edu.stanford.protege.webprotege.perspective;

import edu.stanford.protege.webprotege.api.ActionExecutor;
import edu.stanford.protege.webprotege.ipc.CommandHandler;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.WebProtegeHandler;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-08-21
 */
@WebProtegeHandler
public class SetPerspectivesCommandHandler implements CommandHandler<SetPerspectivesAction, SetPerspectivesResult> {

    private final ActionExecutor executor;

    public SetPerspectivesCommandHandler(ActionExecutor executor) {
        this.executor = executor;
    }

    @NotNull
    @Override
    public String getChannelName() {
        return SetPerspectivesAction.CHANNEL;
    }

    @Override
    public Class<SetPerspectivesAction> getRequestClass() {
        return SetPerspectivesAction.class;
    }

    @Override
    public Mono<SetPerspectivesResult> handleRequest(SetPerspectivesAction request, ExecutionContext executionContext) {
        return Mono.just(executor.execute(request, executionContext));
    }
}