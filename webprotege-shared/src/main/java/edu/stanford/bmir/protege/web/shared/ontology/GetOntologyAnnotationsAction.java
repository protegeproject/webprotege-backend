package edu.stanford.bmir.protege.web.shared.ontology;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import edu.stanford.bmir.protege.web.shared.dispatch.ProjectAction;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 21/02/2013
 */
@AutoValue

@JsonTypeName("GetOntologyAnnotations")
public abstract class GetOntologyAnnotationsAction implements ProjectAction<GetOntologyAnnotationsResult> {

    @JsonCreator
    public static GetOntologyAnnotationsAction create(@JsonProperty("projectId") @Nonnull ProjectId projectId,
                                                      @JsonProperty("ontologyId") @Nonnull Optional<OWLOntologyID> ontologyId) {
        return new AutoValue_GetOntologyAnnotationsAction(projectId, ontologyId);
    }

    @Nonnull
    public abstract ProjectId getProjectId();

    @Nonnull
    public abstract Optional<OWLOntologyID> getOntologyId();
}