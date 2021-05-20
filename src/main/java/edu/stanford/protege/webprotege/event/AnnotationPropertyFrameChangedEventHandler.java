package edu.stanford.protege.webprotege.event;



/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 18/12/2012
 */
public interface AnnotationPropertyFrameChangedEventHandler extends EventHandler {

    void annotationPropertyFrameChanged(AnnotationPropertyFrameChangedEvent event);
}