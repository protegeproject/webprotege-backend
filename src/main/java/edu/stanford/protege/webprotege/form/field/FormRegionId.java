package edu.stanford.protege.webprotege.form.field;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2020-04-22
 */
@JsonSubTypes({
        @Type(FormFieldId.class),
        @Type(GridColumnId.class)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface FormRegionId {

    String getId();
}