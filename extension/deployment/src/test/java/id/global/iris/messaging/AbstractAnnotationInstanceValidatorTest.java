package id.global.iris.messaging;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

public abstract class AbstractAnnotationInstanceValidatorTest extends BaseIndexingTest {

    protected AnnotationInstance getAnnotationInstance(Class<?> annotationClass, Class<?>... annotatedClasses) {
        return indexOf(annotatedClasses)
                .getAnnotations(DotName.createSimple(annotationClass.getCanonicalName()))
                .get(0);
    }

}
