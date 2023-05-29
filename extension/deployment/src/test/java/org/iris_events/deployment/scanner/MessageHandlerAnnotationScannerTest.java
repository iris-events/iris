package org.iris_events.deployment.scanner;

import static org.iris_events.annotations.ExchangeType.FANOUT;
import static org.iris_events.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.BaseIndexingTest;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.validation.AnnotationInstanceValidator;
import org.iris_events.test.Event;
import org.iris_events.test.PriorityQueueEvent;
import org.iris_events.test.TestHandlerService;

class MessageHandlerAnnotationScannerTest extends BaseIndexingTest {

    public static final String FANOUT_EXCHANGE = "fanout-exchange";
    public static final String TOPIC_EXCHANGE = "topic-exchange";

    private MessageHandlerAnnotationScanner annotationScanner;

    @BeforeEach
    void beforeEach() {
        final var annotationInstanceValidator = Mockito.mock(AnnotationInstanceValidator.class);
        annotationScanner = new MessageHandlerAnnotationScanner(annotationInstanceValidator);
    }

    @Test
    public void messageHandlerScannerShouldScanServiceAnnotations() {
        final var messageHandlerInfoBuildItems = scanService(
                MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        assertNotNull(messageHandlerInfoBuildItems);
        assertEquals(4, messageHandlerInfoBuildItems.size());
    }

    @Test
    public void messageHandlerScannerShouldScanQueueParameter() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(
                MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        final List<MessageHandlerInfoBuildItem> handleCustomQueueParam = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName()
                        .equals("handleCustomQueueParam"))
                .toList();

        final List<MessageHandlerInfoBuildItem> handle = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName().equals("handle"))
                .toList();

        assertEquals(1, handleCustomQueueParam.size());
        assertEquals(1, handle.size());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handleCustomQueueParam.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals("handleCustomQueueParam", messageHandlerInfoBuildItem.getMethodName());
        Assertions.assertEquals(TestHandlerService.EVENT_QUEUE_PRIORITY, messageHandlerInfoBuildItem.getBindingKeys().get(0));

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItemClassName = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItemClassName);
        assertEquals("handle", messageHandlerInfoBuildItemClassName.getMethodName());
        assertEquals("event-queue", messageHandlerInfoBuildItemClassName.getBindingKeys().get(0));
    }

    @Test
    public void messageHandlerScannerShouldScanFanoutAndTopicExchanges() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(
                MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        MessageHandlerInfoBuildItem fanoutBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(FANOUT))
                .toList()
                .get(0);

        MessageHandlerInfoBuildItem topicBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(TOPIC))
                .toList()
                .get(0);

        assertNotNull(fanoutBuildItem);
        assertNotNull(topicBuildItem);

        assertEquals(FANOUT_EXCHANGE, fanoutBuildItem.getName());
        assertEquals(TOPIC_EXCHANGE, topicBuildItem.getName());
        assertEquals(FANOUT, fanoutBuildItem.getExchangeType());
        assertEquals(TOPIC, topicBuildItem.getExchangeType());

        assertNull(fanoutBuildItem.getBindingKeys());
        List<String> bindingKeys = topicBuildItem.getBindingKeys();
        assertNotNull(bindingKeys);
        assertTrue(bindingKeys.contains("lazy.yellow"));
        assertTrue(bindingKeys.contains("*.*.rabbit"));
    }

    @Test
    public void messageHandlerScannerShouldScanParameterClass() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(TestHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        assertEquals(2, messageHandlerInfoBuildItems.size());

        List<MessageHandlerInfoBuildItem> handle = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName().equals("handle"))
                .toList();

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals(Type.create(DotName.createSimple(Event.class.getName()), Type.Kind.CLASS),
                messageHandlerInfoBuildItem.getParameterType());
    }

    private List<MessageHandlerInfoBuildItem> scanService(Class<?>... classes) {
        final IndexView index = indexOf(classes);
        return annotationScanner.scanHandlerAnnotations(index);
    }

    @SuppressWarnings("unused")
    public static class MessageHandlerService {

        @MessageHandler(bindingKeys = "event-queue")
        public void handle(Event event) {
            System.out.println("Handling event");
        }

        @MessageHandler(bindingKeys = "event-queue-priority")
        public void handleCustomQueueParam(PriorityQueueEvent event) {
            System.out.println("Handling priority event");
        }

        @MessageHandler
        public void handleFanout(FanoutEvent event) {
            System.out.println("Handling fanout event");
        }

        // TODO: check if "lazy.yellow" should be allowed
        @MessageHandler(bindingKeys = { "lazy.yellow", "*.*.rabbit" })
        public void handleTopic(TopicEvent event) {
            System.out.println("Handling topic event");
        }
    }

    @Message(name = FANOUT_EXCHANGE, exchangeType = FANOUT)
    public record FanoutEvent() {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC)
    public record TopicEvent() {
    }

}
