package edu.stanford.protege.webprotege.entity;

import edu.stanford.protege.webprotege.index.AnnotationAssertionAxiomsByValueIndex;
import edu.stanford.protege.webprotege.index.EntitiesInProjectSignatureByIriIndex;
import edu.stanford.protege.webprotege.index.ProjectOntologiesIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntologyID;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SubjectClosureResolver_TestCase {

    private SubjectClosureResolver resolver;

    @Mock
    private AnnotationAssertionAxiomsByValueIndex axiomsByValueIndex;

    @Mock
    private ProjectOntologiesIndex projectOntologiesIndex;

    @Mock
    private EntitiesInProjectSignatureByIriIndex entitiesInSignatureIndex;

    private IRI valueIri = IRI.create("http://example.org/Other");

    private OWLEntity valueEntity = new OWLClassImpl(valueIri);

    private final IRI clsIri = IRI.create("http://example.org/Cls");

    private final OWLEntity cls = new OWLClassImpl(clsIri);

    @Mock
    private OWLAnnotationAssertionAxiom axiom;

    @Mock
    private OWLOntologyID ontologyId;


    @Before
    public void setUp() throws Exception {
        resolver = new SubjectClosureResolver(axiomsByValueIndex,
                                              projectOntologiesIndex,
                                              entitiesInSignatureIndex);
        when(axiomsByValueIndex.getAxiomsByValue(valueIri, ontologyId))
                .thenAnswer(invocation -> Stream.of(axiom));
        when(entitiesInSignatureIndex.getEntitiesInSignature(clsIri))
                .thenAnswer(invocation -> Stream.of(cls));
        when(projectOntologiesIndex.getOntologyIds())
                .thenAnswer(invocation -> Stream.of(ontologyId));
    }

    @Test
    public void shouldGetClsAsRootEntity() {
        when(axiom.getSubject()).thenReturn(clsIri);
        var rootEntities = resolver.resolve(valueEntity)
                .collect(toSet());
        assertThat(rootEntities, hasItems(cls, valueEntity));
    }

    @Test
    public void shouldResolveSelf() {
        var rootEntities = resolver.resolve(cls).collect(toSet());
        assertThat(rootEntities, contains(cls));

    }

    @Test
    public void shouldTerminateWithLoops() {
        when(axiom.getSubject()).thenReturn(valueIri);
        resolver.resolve(valueEntity);
        // Should complete
    }
}