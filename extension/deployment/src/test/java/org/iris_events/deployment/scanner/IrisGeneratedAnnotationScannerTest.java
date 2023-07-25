package org.iris_events.deployment.scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.iris_events.BaseIndexingTest;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IrisGeneratedAnnotationScannerTest extends BaseIndexingTest {

    private IrisGeneratedAnnotationScanner irisGeneratedAnnotationScanner;

    @BeforeEach
    void beforeEach() {
        this.irisGeneratedAnnotationScanner = new IrisGeneratedAnnotationScanner();
    }

    @Test
    void scanMessageAnnotations() {
        final var messageHandlerInfoBuildItems = scanService(
                MessageHandlerService.class,
                Event.class,
                IrisGeneratedEvent.class);

        assertThat(messageHandlerInfoBuildItems, is(notNullValue()));
        assertThat(messageHandlerInfoBuildItems.size(), is(1));
        assertThat(messageHandlerInfoBuildItems.get(0).getAnnotatedClassInfo().name().toString(),
                is("org.iris_events.deployment.scanner.IrisGeneratedAnnotationScannerTest$IrisGeneratedEvent"));
    }

    private List<MessageInfoBuildItem> scanService(Class<?>... classes) {
        final IndexView index = basicIndexOf(classes);
        return irisGeneratedAnnotationScanner.scanIrisGeneratedAnnotations(index);
    }

    @SuppressWarnings("unused")
    public static class MessageHandlerService {

        @MessageHandler()
        public void handle(Event event) {
            System.out.println("Handling event");
        }

        @MessageHandler()
        public void handleGenerated(IrisGeneratedEvent event) {
            System.out.println("Handling generated event");
        }

    }

    @Message(name = "event")
    public record Event() {
    }

    @IrisGenerated
    @Message(name = "iris-generated")
    public record IrisGeneratedEvent() {
    }
}
