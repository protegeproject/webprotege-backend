package edu.stanford.protege.webprotege.project;

import edu.stanford.protege.webprotege.authorization.*;
import edu.stanford.protege.webprotege.ipc.*;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;

import static edu.stanford.protege.webprotege.access.BuiltInAction.*;


@WebProtegeHandler
public class CreateNewProjectFromProjectBackupCommandHandler implements AuthorizedCommandHandler<CreateNewProjectFromProjectBackupAction, CreateNewProjectFromProjectBackupResult> {


    private final CreateProjectSagaManager createProjectSagaManager;


    public CreateNewProjectFromProjectBackupCommandHandler(CreateProjectSagaManager createProjectSagaManager) {
        this.createProjectSagaManager = createProjectSagaManager;
    }

    @Nonnull
    @Override
    public String getChannelName() {
        return CreateNewProjectFromProjectBackupAction.CHANNEL;
    }

    @Override
    public Class<CreateNewProjectFromProjectBackupAction> getRequestClass() {
        return CreateNewProjectFromProjectBackupAction.class;
    }

    @Override
    public Mono<CreateNewProjectFromProjectBackupResult> handleRequest(CreateNewProjectFromProjectBackupAction request,
                                                                       ExecutionContext executionContext) {
        var result = createProjectSagaManager.executeFromBackup(request.newProjectSettings(), request.branchName(), executionContext);
        return Mono.fromFuture(result);
    }

    @Nonnull
    @Override
    public Resource getTargetResource(CreateNewProjectFromProjectBackupAction request) {
        return ApplicationResource.get();
    }

    @Nonnull
    @Override
    public Collection<ActionId> getRequiredCapabilities() {
        return List.of(CREATE_EMPTY_PROJECT.getActionId(), UPLOAD_PROJECT.getActionId());
    }


}
