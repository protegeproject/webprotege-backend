package edu.stanford.protege.webprotege.obo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

import edu.stanford.protege.webprotege.dispatch.Result;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 22 Jun 2017
 */
@AutoValue

@JsonTypeName("SetOboTermSynonymsResult")
public abstract class SetOboTermSynonymsResult implements Result {

    @JsonCreator
    public static SetOboTermSynonymsResult create() {
        return new AutoValue_SetOboTermSynonymsResult();
    }
}