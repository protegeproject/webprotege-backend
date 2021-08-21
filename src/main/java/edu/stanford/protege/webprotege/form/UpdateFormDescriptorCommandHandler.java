package edu.stanford.protege.webprotege.form;

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
public class UpdateFormDescriptorCommandHandler implements CommandHandler<UpdateFormDescriptorAction, UpdateFormDescriptorResult> {

    private final ActionExecutor executor;

    public UpdateFormDescriptorCommandHandler(ActionExecutor executor) {
        this.executor = executor;
    }

    @NotNull
    @Override
    public String getChannelName() {
        return UpdateFormDescriptorAction.CHANNEL;
    }

    @Override
    public Class<UpdateFormDescriptorAction> getRequestClass() {
        return UpdateFormDescriptorAction.class;
    }

    @Override
    public Mono<UpdateFormDescriptorResult> handleRequest(UpdateFormDescriptorAction request,
                                                          ExecutionContext executionContext) {
        return Mono.just(executor.execute(request, executionContext));
    }
}