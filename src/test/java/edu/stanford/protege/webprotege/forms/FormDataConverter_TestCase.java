package edu.stanford.protege.webprotege.forms;

import edu.stanford.protege.webprotege.forms.data.FormData;
import edu.stanford.protege.webprotege.forms.processor.FormDataConverter;
import edu.stanford.protege.webprotege.forms.processor.FormDataProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2020-04-26
 */
@RunWith(MockitoJUnitRunner.class)
public class FormDataConverter_TestCase {

    private FormDataConverter converter;

    @Mock
    private FormFrameBuilder formFrameBuilder;

    @Mock
    private FormSubjectResolver formSubjectResolver;

    @Mock
    private FormDataProcessor formDataProcessor;

    @Mock
    private FormData formData;

    @Mock
    private FormFrame formFrame;

    @Before
    public void setUp() {
        converter = new FormDataConverter(
                formSubjectResolver,
                formDataProcessor
        );
        when(formDataProcessor.processFormData(formData, false))
                .thenReturn(formFrameBuilder);
        when(formFrameBuilder.build(formSubjectResolver))
                .thenReturn(formFrame);
    }

    @Test
    public void shouldCreateFormFrame() {
        // Check that the converter uses the supplied form data processor and resolver correctly
        var converted = converter.convert(formData);
        assertThat(converted, is(formFrame));
    }
}
