package id.global.event.messaging.deployment;

import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.BaseIndexingTest;
import id.global.event.messaging.deployment.scanner.MessageHandlerScanner;
import id.global.event.messaging.test.Event;
import id.global.event.messaging.test.PriorityQueueEvent;
import id.global.event.messaging.test.TestHandlerService;

public class MessageHandlerScannerTest extends BaseIndexingTest {
    public static final String FANOUT_EXCHANGE = "fanout-exchange";
    public static final String TOPIC_EXCHANGE = "topic-exchange";

    public static class MessageHandlerService {

        @MessageHandler
        public void handle(Event event) {
            System.out.println("Handling event");
        }

        @MessageHandler
        public void handleCustomQueueParam(PriorityQueueEvent event) {
            System.out.println("Handling priority event");
        }

        @MessageHandler
        public void handleFanout(FanoutEvent event) {
            System.out.println("Handling fanout event");
        }

        @MessageHandler
        public void handleTopic(TopicEvent event) {
            System.out.println("Handling topic event");
        }
    }

    @ConsumedEvent(exchange = FANOUT_EXCHANGE, exchangeType = FANOUT)
    public record FanoutEvent() {
    }

    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "lazy.yellow", "*.*.rabbit" })
    public record TopicEvent() {
    }

    @Test
    public void messageHandlerScannerShouldScanServiceAnnotations() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        assertNotNull(messageHandlerInfoBuildItems);
        assertEquals(4, messageHandlerInfoBuildItems.size());
    }

    @Test
    public void messageHandlerScannerShouldScanQueueParameter() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        final List<MessageHandlerInfoBuildItem> handleCustomQueueParam = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName()
                        .equals("handleCustomQueueParam"))
                .collect(Collectors.toList());

        final List<MessageHandlerInfoBuildItem> handle = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName().equals("handle"))
                .collect(Collectors.toList());

        assertEquals(1, handleCustomQueueParam.size());
        assertEquals(1, handle.size());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handleCustomQueueParam.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals("handleCustomQueueParam", messageHandlerInfoBuildItem.getMethodName());
        Assertions.assertEquals(TestHandlerService.EVENT_QUEUE_PRIORITY, messageHandlerInfoBuildItem.getQueue());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItemClassName = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItemClassName);
        assertEquals("handle", messageHandlerInfoBuildItemClassName.getMethodName());
        assertEquals("event-queue", messageHandlerInfoBuildItemClassName.getQueue());
    }

    @Test
    public void messageHandlerScannerShoudScanFanoutAndTopicExchanges() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class);

        MessageHandlerInfoBuildItem fanoutBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(FANOUT)).collect(Collectors.toList())
                .get(0);

        MessageHandlerInfoBuildItem topicBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(TOPIC)).collect(Collectors.toList())
                .get(0);

        assertNotNull(fanoutBuildItem);
        assertNotNull(topicBuildItem);

        assertEquals(FANOUT_EXCHANGE, fanoutBuildItem.getExchange());
        assertEquals(TOPIC_EXCHANGE, topicBuildItem.getExchange());
        assertEquals(FANOUT, fanoutBuildItem.getExchangeType());
        assertEquals(TOPIC, topicBuildItem.getExchangeType());

        assertNull(fanoutBuildItem.getBindingKeys());
        String[] bindingKeys = topicBuildItem.getBindingKeys();
        assertNotNull(bindingKeys);
        assertTrue(Arrays.asList(bindingKeys).contains("lazy.yellow"));
        assertTrue(Arrays.asList(bindingKeys).contains("*.*.rabbit"));
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
                .collect(Collectors.toList());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals(Type.create(DotName.createSimple(Event.class.getName()), Type.Kind.CLASS),
                messageHandlerInfoBuildItem.getParameterType());
    }

    @Test
    public void messageHandlerScannerShouldFailOnExternalDependencyEvents() {
        MessageHandlerValidationException messageHandlerValidationException = assertThrows(
                MessageHandlerValidationException.class,
                () -> scanService(TestHandlerService.class));
        assertNotNull(messageHandlerValidationException);
        String expectedMessage = String.format(
                "MessageHandler annotated method %s::%s can not have external dependency classes as parameters.",
                TestHandlerService.class.getName(), "handle");
        assertEquals(expectedMessage, messageHandlerValidationException.getMessage());
    }

    private List<MessageHandlerInfoBuildItem> scanService(Class<?>... classes) {
        IndexView index = indexOf(classes);
        MessageHandlerScanner scanner = new MessageHandlerScanner(index);
        return scanner.scanMessageHandlerAnnotations();
    }

}
