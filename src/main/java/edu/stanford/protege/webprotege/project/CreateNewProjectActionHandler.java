package edu.stanford.protege.webprotege.project;

import edu.stanford.protege.webprotege.access.AccessManager;
import edu.stanford.protege.webprotege.access.ApplicationResource;
import edu.stanford.protege.webprotege.access.ProjectResource;
import edu.stanford.protege.webprotege.app.UserInSessionFactory;
import edu.stanford.protege.webprotege.dispatch.ApplicationActionHandler;
import edu.stanford.protege.webprotege.dispatch.ExecutionContext;
import edu.stanford.protege.webprotege.dispatch.RequestContext;
import edu.stanford.protege.webprotege.dispatch.RequestValidator;
import edu.stanford.protege.webprotege.dispatch.validators.ApplicationPermissionValidator;
import edu.stanford.protege.webprotege.dispatch.validators.CompositeRequestValidator;
import edu.stanford.protege.webprotege.dispatch.validators.UserIsSignedInValidator;
import edu.stanford.protege.webprotege.permissions.PermissionDeniedException;
import edu.stanford.protege.webprotege.user.UserId;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.protege.webprotege.access.Subject.forAnySignedInUser;
import static edu.stanford.protege.webprotege.access.Subject.forUser;
import static edu.stanford.protege.webprotege.access.BuiltInAction.CREATE_EMPTY_PROJECT;
import static edu.stanford.protege.webprotege.access.BuiltInAction.UPLOAD_PROJECT;
import static edu.stanford.protege.webprotege.access.BuiltInRole.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 21/02/15
 */
public class CreateNewProjectActionHandler implements ApplicationActionHandler<CreateNewProjectAction, CreateNewProjectResult> {

    private final ProjectManager pm;

    private final ProjectDetailsManager projectDetailsManager;

    private final AccessManager accessManager;

    @Nonnull
    private final UserInSessionFactory userInSessionFactory;

    @Inject
    public CreateNewProjectActionHandler(@Nonnull ProjectManager pm,
                                         @Nonnull ProjectDetailsManager projectDetailsManager,
                                         @Nonnull AccessManager accessManager,
                                         @Nonnull UserInSessionFactory userInSessionFactory) {
        this.pm = checkNotNull(pm);
        this.projectDetailsManager = checkNotNull(projectDetailsManager);
        this.accessManager = checkNotNull(accessManager);
        this.userInSessionFactory = checkNotNull(userInSessionFactory);
    }

    @Nonnull
    @Override
    public Class<CreateNewProjectAction> getActionClass() {
        return CreateNewProjectAction.class;
    }

    @Nonnull
    @Override
    public RequestValidator getRequestValidator(@Nonnull CreateNewProjectAction action, @Nonnull RequestContext requestContext) {
        return new CompositeRequestValidator(
                new UserIsSignedInValidator(requestContext.getUserId()),
                new ApplicationPermissionValidator(
                        accessManager,
                        requestContext.getUserId(),
                        CREATE_EMPTY_PROJECT)
        );
    }

    @Nonnull
    @Override
    public CreateNewProjectResult execute(@Nonnull CreateNewProjectAction action, @Nonnull ExecutionContext executionContext) {
        try {
            UserId userId = executionContext.getUserId();
            if (!accessManager.hasPermission(forUser(userId), ApplicationResource.get(), CREATE_EMPTY_PROJECT)) {
                throw new PermissionDeniedException("You do not have permission to create new projects",
                                                    userInSessionFactory.getUserInSession(userId));
            }
            NewProjectSettings newProjectSettings = action.getNewProjectSettings();
            if (newProjectSettings.hasSourceDocument()) {
                if (!accessManager.hasPermission(forUser(userId), ApplicationResource.get(), UPLOAD_PROJECT)) {
                    throw new PermissionDeniedException("You do not have permission to upload projects",
                                                        userInSessionFactory.getUserInSession(userId));
                }
            }
            ProjectId projectId = pm.createNewProject(newProjectSettings);
            if (!projectDetailsManager.isExistingProject(projectId)) {
                projectDetailsManager.registerProject(projectId, newProjectSettings);
                applyDefaultPermissions(projectId, userId);
            }
            return new CreateNewProjectResult(projectDetailsManager.getProjectDetails(projectId));
        } catch (OWLOntologyCreationException | OWLOntologyStorageException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyDefaultPermissions(ProjectId projectId, UserId userId) {
        ProjectResource projectResource = new ProjectResource(projectId);
        // Owner is manager
        accessManager.setAssignedRoles(forUser(userId),
                                       projectResource,
                                       asList(CAN_MANAGE.getRoleId(), PROJECT_DOWNLOADER.getRoleId()));
        // Any signed in user can edit the layout
        accessManager.setAssignedRoles(forAnySignedInUser(),
                                       projectResource,
                                       singleton(LAYOUT_EDITOR.getRoleId()));
    }


}