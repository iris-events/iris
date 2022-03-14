package id.global.iris.messaging.deployment;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.iris.messaging.BaseIndexingTest;
import id.global.iris.messaging.deployment.scanner.MessageHandlerScanner;
import id.global.iris.messaging.test.Event;
import id.global.iris.messaging.test.PriorityQueueEvent;
import id.global.iris.messaging.test.TestHandlerService;

public class MessageHandlerScannerTest extends BaseIndexingTest {
    public static final String FANOUT_EXCHANGE = "fanout-exchange";
    public static final String TOPIC_EXCHANGE = "topic-exchange";

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

    @Test
    public void messageHandlerScannerShouldScanServiceAnnotations() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class,
                Message.class);

        assertNotNull(messageHandlerInfoBuildItems);
        assertEquals(4, messageHandlerInfoBuildItems.size());
    }

    @Test
    public void messageHandlerScannerShouldScanQueueParameter() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class,
                Message.class,
                MessageHandler.class);

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
        Assertions.assertEquals(TestHandlerService.EVENT_QUEUE_PRIORITY, messageHandlerInfoBuildItem.getBindingKeys().get(0));

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItemClassName = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItemClassName);
        assertEquals("handle", messageHandlerInfoBuildItemClassName.getMethodName());
        assertEquals("event-queue", messageHandlerInfoBuildItemClassName.getBindingKeys().get(0));
    }

    @Test
    public void messageHandlerScannerShouldScanFanoutAndTopicExchanges() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class,
                PriorityQueueEvent.class,
                FanoutEvent.class,
                TopicEvent.class,
                Event.class,
                Message.class);

        MessageHandlerInfoBuildItem fanoutBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(FANOUT)).collect(Collectors.toList())
                .get(0);

        MessageHandlerInfoBuildItem topicBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(TOPIC)).collect(Collectors.toList())
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
                Event.class,
                Message.class);

        assertEquals(2, messageHandlerInfoBuildItems.size());

        List<MessageHandlerInfoBuildItem> handle = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName().equals("handle"))
                .collect(Collectors.toList());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals(Type.create(DotName.createSimple(Event.class.getName()), Type.Kind.CLASS),
                messageHandlerInfoBuildItem.getParameterType());
    }

    private List<MessageHandlerInfoBuildItem> scanService(Class<?>... classes) {
        IndexView index = indexOf(classes);
        MessageHandlerScanner scanner = new MessageHandlerScanner(index);
        return scanner.scanMessageHandlerAnnotations();
    }

}
