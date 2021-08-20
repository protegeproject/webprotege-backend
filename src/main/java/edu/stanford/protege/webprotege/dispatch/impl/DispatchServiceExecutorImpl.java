package edu.stanford.protege.webprotege.dispatch.impl;

import edu.stanford.protege.webprotege.app.UserInSessionFactory;
import edu.stanford.protege.webprotege.dispatch.*;
import edu.stanford.protege.webprotege.permissions.PermissionDeniedException;
import edu.stanford.protege.webprotege.project.HasProjectId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.project.ProjectManager;
import edu.stanford.protege.webprotege.common.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 19/02/2013
 */
public class DispatchServiceExecutorImpl implements DispatchServiceExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceExecutorImpl.class.getName());

    @Nonnull
    private final ApplicationActionHandlerRegistry handlerRegistry;

    @Nonnull
    private final ProjectManager projectManager;

    @Nonnull
    private final UserInSessionFactory userInSessionFactory;

    @Inject
    public DispatchServiceExecutorImpl(@Nonnull ApplicationActionHandlerRegistry handlerRegistry,
                                       @Nonnull ProjectManager projectManager,
                                       @Nonnull UserInSessionFactory userInSessionFactory) {
        this.handlerRegistry = checkNotNull(handlerRegistry);
        this.projectManager = checkNotNull(projectManager);
        this.userInSessionFactory = userInSessionFactory;
    }

    /**
     * Sets the name of a thread so that it contains details of the action (and target project) being executed
     *
     * @param thread    The thread.
     * @param action    The action.
     * @param projectId The optional project id.
     */
    private static void setTemporaryThreadName(@Nonnull Thread thread,
                                               @Nonnull Action<?> action,
                                               @Nullable ProjectId projectId) {
        String tempThreadName;
        final ProjectId targetProjectId;
        if (projectId != null) {
            targetProjectId = projectId;
        }
        else if (action instanceof HasProjectId) {
            targetProjectId = ((HasProjectId) action).getProjectId();
        }
        else {
            targetProjectId = null;
        }
        if (targetProjectId == null) {
            tempThreadName = String.format("[DispatchService] %s",
                                           action.getClass().getSimpleName());
        }
        else {
            tempThreadName = String.format("[DispatchService] %s %s",
                                           action.getClass().getSimpleName(),
                                           targetProjectId);
        }
        thread.setName(tempThreadName);
    }

    @Override
    public <A extends Action<R>, R extends Result> DispatchServiceResultContainer execute(A action, RequestContext requestContext, ExecutionContext executionContext) throws ActionExecutionException, PermissionDeniedException {
        return execAction(action, requestContext, executionContext);
    }

    private <A extends Action<R>, R extends Result> DispatchServiceResultContainer execAction(A action, RequestContext requestContext, ExecutionContext executionContext) {
        final ActionHandler<A, R> actionHandler;
        final Thread thread = Thread.currentThread();
        String threadName = thread.getName();
        if (action instanceof ProjectAction) {
            ProjectAction projectAction = (ProjectAction) action;
            ProjectId projectId = projectAction.getProjectId();
            setTemporaryThreadName(thread, action, projectId);
            ProjectActionHandlerRegistry actionHanderRegistry = projectManager.getActionHandlerRegistry(projectId);
            actionHandler = actionHanderRegistry.getActionHandler(action);
        }
        else {
            setTemporaryThreadName(thread, action, null);
            actionHandler = handlerRegistry.getActionHandler(action);
        }

        RequestValidator validator = actionHandler.getRequestValidator(action, requestContext);
        RequestValidationResult validationResult = validator.validateAction();
        if (!validationResult.isValid()) {
            throw getPermissionDeniedException(requestContext.getUserId(),
                                               validationResult);
        }

        try {
            R result = actionHandler.execute(action, executionContext);
            return DispatchServiceResultContainer.create(result);
        } catch (PermissionDeniedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("An error occurred whilst executing an action", e);
            throw new ActionExecutionException(e);
        } finally {
            thread.setName(threadName);
        }
    }

    private PermissionDeniedException getPermissionDeniedException(@Nonnull UserId userId,
                                                                   @Nonnull RequestValidationResult validationResult) {
        if (validationResult.getInvalidException().isPresent()) {
            Exception validationException = validationResult.getInvalidException().get();
            if (validationException instanceof PermissionDeniedException) {
                return ((PermissionDeniedException) validationException);
            }
        }
        throw new PermissionDeniedException(validationResult.getInvalidMessage(),
                                            userInSessionFactory.getUserInSession(userId));
    }
}
