package edu.stanford.protege.webprotege.watches;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import edu.stanford.protege.webprotege.HasUserId;
import edu.stanford.protege.webprotege.common.Request;
import edu.stanford.protege.webprotege.dispatch.ProjectAction;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 20/03/2013
 */
@AutoValue

@JsonTypeName("GetEntityWatches")
public abstract class GetEntityWatchesAction implements ProjectAction<GetEntityWatchesResult>, HasUserId, Request<GetEntityWatchesResult> {

    public static final String CHANNEL = "watches.GetEntityWatches";

    @JsonCreator
    public static GetEntityWatchesAction create(@JsonProperty("projectId") ProjectId projectId,
                                                @JsonProperty("userId") UserId userId,
                                                @JsonProperty("entity") OWLEntity entity) {
        return new AutoValue_GetEntityWatchesAction(projectId, userId, entity);
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    /**
     * Gets the {@link ProjectId}.
     * @return The {@link ProjectId}.  Not {@code null}.
     */
    @Nonnull
    @Override
    public abstract ProjectId getProjectId();

    /**
     * Gets the {@link UserId}.
     * @return The {@link UserId}.  Not {@code null}.
     */
    @Override
    public abstract UserId getUserId();

    public abstract OWLEntity getEntity();
}