package id.global.iris.messaging.deployment.scanner;

import java.util.Collection;
import java.util.List;

import org.jboss.jandex.IndexView;

import id.global.iris.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.iris.messaging.deployment.validation.MessageHandlerAnnotationInstanceValidator;
import id.global.iris.messaging.deployment.validation.SnapshotHandlerAnnotationInstanceValidator;

public class Scanner {

    private final IndexView indexView;
    private final List<AnnotationScanner> annotationScanners;

    public Scanner(IndexView indexView, String serviceName) {
        this.indexView = indexView;
        this.annotationScanners = getAnnotationScanners(serviceName);
    }

    public List<MessageHandlerInfoBuildItem> scanEventHandlerAnnotations() {
        return annotationScanners.stream()
                .map(messageHandlerScanner -> messageHandlerScanner.scanHandlerAnnotations(indexView))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<AnnotationScanner> getAnnotationScanners(String serviceName) {
        final var snapshotMessageHandlerAnnotationScanner = getSnapshotMessageHandlerAnnotationScanner(serviceName);

        final var messageHandlerAnnotationScanner = getMessageHandlerAnnotationScanner(serviceName);

        return List.of(snapshotMessageHandlerAnnotationScanner, messageHandlerAnnotationScanner);
    }

    private MessageHandlerAnnotationScanner getMessageHandlerAnnotationScanner(String serviceName) {
        final var messageHandlerAnnotationInstanceValidator = new MessageHandlerAnnotationInstanceValidator(indexView,
                serviceName);
        return new MessageHandlerAnnotationScanner(messageHandlerAnnotationInstanceValidator);
    }

    private SnapshotMessageHandlerAnnotationScanner getSnapshotMessageHandlerAnnotationScanner(String serviceName) {
        final var snapshotHandlerAnnotationInstanceValidator = new SnapshotHandlerAnnotationInstanceValidator(indexView,
                serviceName);
        return new SnapshotMessageHandlerAnnotationScanner(snapshotHandlerAnnotationInstanceValidator);
    }

}
