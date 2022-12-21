package id.global.iris.messaging.deployment.scanner;

import java.util.Collection;
import java.util.List;

import id.global.iris.messaging.deployment.builditem.MessageInfoBuildItem;
import org.jboss.jandex.IndexView;

import id.global.iris.messaging.deployment.builditem.MessageHandlerInfoBuildItem;
import id.global.iris.messaging.deployment.validation.MessageHandlerAnnotationInstanceValidator;
import id.global.iris.messaging.deployment.validation.MessageHandlerCompatibilityValidator;
import id.global.iris.messaging.deployment.validation.SnapshotHandlerAnnotationInstanceValidator;

public class Scanner {

    private final IndexView indexView;
    private final List<HandlerAnnotationScanner> messageHandlerAnnotationScanners;
    private final MessageAnnotationScanner messageAnnotationScanner;
    private final IrisGeneratedAnnotationScanner irisGeneratedAnnotationScanner;

    public Scanner(IndexView indexView, String serviceName) {
        this.indexView = indexView;
        this.messageHandlerAnnotationScanners = getAnnotationScanners(serviceName);
        this.messageAnnotationScanner = new MessageAnnotationScanner();
        this.irisGeneratedAnnotationScanner = new IrisGeneratedAnnotationScanner();
    }

    public List<MessageHandlerInfoBuildItem> scanEventHandlerAnnotations() {
        final var messageHandlerInfoBuildItems = messageHandlerAnnotationScanners.stream()
                .map(messageHandlerScanner -> messageHandlerScanner.scanHandlerAnnotations(indexView))
                .flatMap(Collection::stream)
                .toList();

        MessageHandlerCompatibilityValidator.validate(messageHandlerInfoBuildItems);

        return messageHandlerInfoBuildItems;
    }

    public List<MessageInfoBuildItem> scanMessageAnnotations() {
        return messageAnnotationScanner.scanMessageAnnotations(indexView);
    }

    public List<MessageInfoBuildItem> scanIrisGeneratedAnnotations() {
        return irisGeneratedAnnotationScanner.scanMessageAnnotations(indexView);
    }

    private List<HandlerAnnotationScanner> getAnnotationScanners(String serviceName) {
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
