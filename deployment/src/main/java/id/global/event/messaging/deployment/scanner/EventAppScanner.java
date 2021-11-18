package id.global.event.messaging.deployment.scanner;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EVENT_APP_ID_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EVENT_APP_INFO_DESCRIPTION_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EVENT_APP_INFO_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EVENT_APP_INFO_TITLE_PARAM;

import java.util.Optional;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.event.messaging.runtime.context.EventAppContext;
import io.smallrye.asyncapi.spec.annotations.EventApp;

public class EventAppScanner {

    private final DotName DOT_NAME_EVENT_APP = DotName.createSimple(EventApp.class.getCanonicalName());

    private final IndexView index;

    public EventAppScanner(IndexView index) {
        this.index = index;
    }

    public Optional<EventAppContext> findEventAppContext() {

        final var optionalEventAppAnnotation = index
                .getAnnotations(DOT_NAME_EVENT_APP)
                .stream()
                .findFirst();

        return optionalEventAppAnnotation.map(annotationInstance -> {
            final var id = annotationInstance.value(EVENT_APP_ID_PARAM).asString();
            final var info = annotationInstance.value(EVENT_APP_INFO_PARAM).asNested();
            final var title = info.value(EVENT_APP_INFO_TITLE_PARAM).asString();
            final var description = info.value(EVENT_APP_INFO_DESCRIPTION_PARAM).asString();

            return new EventAppContext(id, title, description);
        });
    }
}
