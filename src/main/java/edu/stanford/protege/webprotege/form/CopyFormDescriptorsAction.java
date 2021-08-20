package edu.stanford.protege.webprotege.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import edu.stanford.protege.webprotege.dispatch.ProjectAction;
import edu.stanford.protege.webprotege.common.ProjectId;

import javax.annotation.Nonnull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2020-04-14
 */
@AutoValue

@JsonTypeName("CopyFormDescriptors")
public abstract class CopyFormDescriptorsAction implements ProjectAction<CopyFormDescriptorsResult> {

    public static final String CHANNEL = "forms.CopyFormDescriptors";

    @Nonnull
    @Override
    public abstract ProjectId getProjectId();

    @Nonnull
    public abstract ProjectId getProjectIdToCopyFrom();

    @Nonnull
    public abstract ImmutableList<FormId> getFormIdsToCopy();

    @JsonCreator
    public static CopyFormDescriptorsAction create(@JsonProperty("projectId") ProjectId newProjectId,
                                                   @JsonProperty("projectIdToCopyFrom") ProjectId newProjectIdToCopyFrom,
                                                   @JsonProperty("formIdsToCopy") ImmutableList<FormId> newFormIdsToCopy) {
        return new AutoValue_CopyFormDescriptorsAction(newProjectId,
                                                       newProjectIdToCopyFrom,
                                                       newFormIdsToCopy);
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
