package edu.stanford.protege.webprotege.entity;

import com.google.common.collect.ImmutableSet;
import edu.stanford.protege.webprotege.access.AccessManager;
import edu.stanford.protege.webprotege.change.ChangeApplicationResult;
import edu.stanford.protege.webprotege.change.ChangeListGenerator;
import edu.stanford.protege.webprotege.change.HasApplyChanges;
import edu.stanford.protege.webprotege.dispatch.AbstractProjectChangeHandler;
import edu.stanford.protege.webprotege.dispatch.ExecutionContext;
import edu.stanford.protege.webprotege.events.EventManager;
import edu.stanford.protege.webprotege.access.BuiltInAction;
import edu.stanford.protege.webprotege.event.EventList;
import edu.stanford.protege.webprotege.event.ProjectEvent;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2020-10-01
 */
public class CreateEntityFromFormDataActionHandler extends AbstractProjectChangeHandler<OWLEntity, CreateEntityFromFormDataAction, CreateEntityFromFormDataResult> {

    private final CreateEntityFromFormDataChangeListGeneratorFactory changeListGeneratorFactory;

    private final EntityNodeRenderer renderer;

    @Inject
    public CreateEntityFromFormDataActionHandler(@Nonnull AccessManager accessManager,
                                                 @Nonnull EventManager<ProjectEvent<?>> eventManager,
                                                 @Nonnull HasApplyChanges applyChanges,
                                                 @Nonnull CreateEntityFromFormDataChangeListGeneratorFactory changeListGeneratorFactory,
                                                 @Nonnull EntityNodeRenderer renderer) {
        super(accessManager, eventManager, applyChanges);
        this.changeListGeneratorFactory = checkNotNull(changeListGeneratorFactory);
        this.renderer = renderer;
    }

    @Nonnull
    @Override
    public Class<CreateEntityFromFormDataAction> getActionClass() {
        return CreateEntityFromFormDataAction.class;
    }

    @Nullable
    @Override
    protected BuiltInAction getRequiredExecutableBuiltInAction(CreateEntityFromFormDataAction action) {
        if(action.getEntityType().equals(EntityType.CLASS)) {
            return BuiltInAction.CREATE_CLASS;
        }
        else if(action.getEntityType().equals(EntityType.OBJECT_PROPERTY)) {
            return BuiltInAction.CREATE_PROPERTY;
        }
        else if(action.getEntityType().equals(EntityType.DATA_PROPERTY)) {
            return BuiltInAction.CREATE_PROPERTY;
        }
        else if(action.getEntityType().equals(EntityType.ANNOTATION_PROPERTY)) {
            return BuiltInAction.CREATE_PROPERTY;
        }
        else if(action.getEntityType().equals(EntityType.NAMED_INDIVIDUAL)) {
            return BuiltInAction.CREATE_INDIVIDUAL;
        }
        else if (action.getEntityType().equals(EntityType.DATATYPE)) {
            return BuiltInAction.CREATE_DATATYPE;
        }
        else {
            throw new RuntimeException("Unknown entity");
        }
    }

    @Override
    protected ChangeListGenerator<OWLEntity> getChangeListGenerator(CreateEntityFromFormDataAction action,
                                                                    ExecutionContext executionContext) {
        return changeListGeneratorFactory.create(action.getEntityType(),
                                                 action.getFreshEntityIri(),
                                                 action.getFormData());
    }

    @Override
    protected CreateEntityFromFormDataResult createActionResult(ChangeApplicationResult<OWLEntity> changeApplicationResult,
                                                                CreateEntityFromFormDataAction action,
                                                                ExecutionContext executionContext,
                                                                EventList<ProjectEvent<?>> eventList) {

        var entityNodes = ImmutableSet.of(renderer.render(changeApplicationResult.getSubject()));
        return CreateEntityFromFormDataResult.create(action.getProjectId(),
                                                  eventList,
                                                  entityNodes);
    }

}