package edu.stanford.protege.webprotege.dispatch.handlers;

import edu.stanford.protege.webprotege.access.AccessManager;
import edu.stanford.protege.webprotege.change.ChangeApplicationResult;
import edu.stanford.protege.webprotege.change.ChangeListGenerator;
import edu.stanford.protege.webprotege.change.CreateAnnotationPropertiesChangeGeneratorFactory;
import edu.stanford.protege.webprotege.change.HasApplyChanges;
import edu.stanford.protege.webprotege.dispatch.AbstractProjectChangeHandler;
import edu.stanford.protege.webprotege.dispatch.ExecutionContext;
import edu.stanford.protege.webprotege.entity.EntityNodeRenderer;
import edu.stanford.protege.webprotege.events.EventManager;
import edu.stanford.protege.webprotege.renderer.RenderingManager;
import edu.stanford.protege.webprotege.access.BuiltInAction;
import edu.stanford.protege.webprotege.entity.CreateAnnotationPropertiesAction;
import edu.stanford.protege.webprotege.entity.CreateAnnotationPropertiesResult;
import edu.stanford.protege.webprotege.event.EventList;
import edu.stanford.protege.webprotege.event.ProjectEvent;
import edu.stanford.protege.webprotege.project.ProjectId;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static edu.stanford.protege.webprotege.access.BuiltInAction.CREATE_PROPERTY;
import static edu.stanford.protege.webprotege.access.BuiltInAction.EDIT_ONTOLOGY;
import static java.util.Arrays.asList;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 25/03/2013
 */
public class CreateAnnotationPropertiesActionHandler extends AbstractProjectChangeHandler<Set<OWLAnnotationProperty>, CreateAnnotationPropertiesAction, CreateAnnotationPropertiesResult> {

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final CreateAnnotationPropertiesChangeGeneratorFactory changeGeneratorFactory;

    @Nonnull
    private final EntityNodeRenderer entityNodeRenderer;

    @Inject
    public CreateAnnotationPropertiesActionHandler(@Nonnull AccessManager accessManager,
                                                   @Nonnull EventManager<ProjectEvent<?>> eventManager,
                                                   @Nonnull HasApplyChanges applyChanges,
                                                   @Nonnull ProjectId projectId,
                                                   @Nonnull RenderingManager renderer,
                                                   @Nonnull CreateAnnotationPropertiesChangeGeneratorFactory changeGeneratorFactory, @Nonnull EntityNodeRenderer entityNodeRenderer) {
        super(accessManager, eventManager, applyChanges);
        this.projectId = checkNotNull(projectId);
        this.changeGeneratorFactory = checkNotNull(changeGeneratorFactory);
        this.entityNodeRenderer = checkNotNull(entityNodeRenderer);
    }

    @Override
    protected ChangeListGenerator<Set<OWLAnnotationProperty>> getChangeListGenerator(CreateAnnotationPropertiesAction action,
                                                                                     ExecutionContext executionContext) {
        return changeGeneratorFactory.create(action.getSourceText(),
                                             action.getLangTag(),
                                             action.getParents());
    }

    @Override
    protected CreateAnnotationPropertiesResult createActionResult(ChangeApplicationResult<Set<OWLAnnotationProperty>> changeApplicationResult,
                                                                  CreateAnnotationPropertiesAction action,
                                                                  ExecutionContext executionContext,
                                                                  EventList<ProjectEvent<?>> eventList) {
        Set<OWLAnnotationProperty> properties = changeApplicationResult.getSubject();
        return CreateAnnotationPropertiesResult.create(projectId,
                                                    properties.stream()
                                                          .map(entityNodeRenderer::render)
                                                          .collect(toImmutableSet()),
                                                    eventList);
    }

    @Nonnull
    @Override
    protected Iterable<BuiltInAction> getRequiredExecutableBuiltInActions(CreateAnnotationPropertiesAction action) {
        return asList(EDIT_ONTOLOGY, CREATE_PROPERTY);
    }

    @Nonnull
    @Override
    public Class<CreateAnnotationPropertiesAction> getActionClass() {
        return CreateAnnotationPropertiesAction.class;
    }
}