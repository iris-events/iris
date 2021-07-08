package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.event.messaging.deployment.MessageHandlerScanner;
import id.global.event.messaging.runtime.enums.ExchangeType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageHandlerAnnotationScannerTest {
    public static final String FANOUT_EXCHANGE = "fanout_exchange";
    public static final String TOPIC_EXCHANGE = "topic_exchange";

    public static class MessageHandlerService {
        public static final String EVENT_QUEUE = "EventQueue";
        public static final String EVENT_QUEUE_PRIORITY = "EventQueue_priority";

        @MessageHandler(queue = EVENT_QUEUE)
        public void handle(Event event) {
            System.out.println("Handling event");
        }

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY)
        public void handleCustomQueueParam(Event event) {
            System.out.println("Handling priority event");
        }

        @FanoutMessageHandler(exchange = FANOUT_EXCHANGE)
        public void handleFanout(Event event) {
            System.out.println("Handling fanout event");
        }

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "lazy.yellow", "*.*.rabbit" })
        public void handleTopic(Event event) {
            System.out.println("Handling topic event");
        }
    }

    @Test
    public void messageHandlerScannerShouldScanServiceAnnotations() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class);

        assertNotNull(messageHandlerInfoBuildItems);
        assertEquals(4, messageHandlerInfoBuildItems.size());
    }

    @Test
    public void messageHandlerScannerShouldScanQueueParameter() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class);

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
        assertEquals(TestHandlerService.EVENT_QUEUE_PRIORITY, messageHandlerInfoBuildItem.getQueue());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItemClassName = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItemClassName);
        assertEquals("handle", messageHandlerInfoBuildItemClassName.getMethodName());
        assertEquals("EventQueue", messageHandlerInfoBuildItemClassName.getQueue());
    }

    @Test
    public void messageHandlerScannerShoudScanFanoutAndTopicExchanges() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(MessageHandlerService.class);

        MessageHandlerInfoBuildItem fanoutBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(ExchangeType.FANOUT)).collect(Collectors.toList())
                .get(0);

        MessageHandlerInfoBuildItem topicBuildItem = messageHandlerInfoBuildItems.stream()
                .filter(buildItem -> buildItem.getExchangeType().equals(ExchangeType.TOPIC)).collect(Collectors.toList())
                .get(0);

        assertNotNull(fanoutBuildItem);
        assertNotNull(topicBuildItem);

        assertEquals(FANOUT_EXCHANGE ,fanoutBuildItem.getExchange());
        assertEquals(TOPIC_EXCHANGE, topicBuildItem.getExchange());
        assertEquals(ExchangeType.FANOUT, fanoutBuildItem.getExchangeType());
        assertEquals(ExchangeType.TOPIC, topicBuildItem.getExchangeType());

        assertNull(fanoutBuildItem.getBindingKeys());
        String[] bindingKeys = topicBuildItem.getBindingKeys();
        assertNotNull(bindingKeys);
        assertTrue(Arrays.asList(bindingKeys).contains("lazy.yellow"));
        assertTrue(Arrays.asList(bindingKeys).contains("*.*.rabbit"));
    }

    @Test
    public void messageHandlerScannerShouldScanParameterClass() {
        final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanService(TestHandlerService.class);

        assertEquals(2, messageHandlerInfoBuildItems.size());

        List<MessageHandlerInfoBuildItem> handle = messageHandlerInfoBuildItems.stream()
                .filter(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getMethodName().equals("handle"))
                .collect(Collectors.toList());

        MessageHandlerInfoBuildItem messageHandlerInfoBuildItem = handle.get(0);
        assertNotNull(messageHandlerInfoBuildItem);
        assertEquals(Type.create(DotName.createSimple(Event.class.getName()), Type.Kind.CLASS),
                messageHandlerInfoBuildItem.getParameterType());
    }

    private List<MessageHandlerInfoBuildItem> scanService(Class<?> serviceClass) {
        IndexView index = indexOf(serviceClass);
        MessageHandlerScanner scanner = new MessageHandlerScanner(index);
        return scanner.scanMessageHandlerAnnotations();
    }

    private Index indexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }

        return indexer.complete();
    }

    private void index(Indexer indexer, String resName) {
        try {
            InputStream stream = tcclGetResourceAsStream(resName);
            indexer.index(stream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private InputStream tcclGetResourceAsStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    private String pathOf(Class<?> clazz) {
        return clazz.getName().replace('.', '/').concat(".class");
    }
}
