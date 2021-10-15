package id.global.event.messaging.runtime.consumer;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;

public class AmqpConsumerTest {

    public static final String PAYLOAD = "testPayload";
    public static final String QUEUE = "testQueue";
    public static final String EXCHANGE = "testExchange";
    public static final String ROUTING_KEY = "MyTestEvent";
    public static final String TEST_METHOD_NAME = "testMethod";

    @Test
    void consumerMethodHandleShouldCorrectlyInvoke() throws NoSuchMethodException, IllegalAccessException, IOException {
        TestEventHandler handler = new TestEventHandler();

        AmqpConsumer consumer = new AmqpConsumer(
                createHandle(),
                new MethodHandleContext(TestEventHandler.class, MyTestEvent.class, TEST_METHOD_NAME),
                new AmqpContext(QUEUE, EXCHANGE, new String[0], DIRECT),
                handler,
                new ObjectMapper(),
                new EventContext());

        MyTestEvent event = new MyTestEvent(PAYLOAD);
        byte[] eventAsBytes = new ObjectMapper().writeValueAsBytes(event);

        assertThat(handler.isEventReceived(), is(false));
        assertThat(handler.getPayload(), is(nullValue()));

        consumer.getCallback().handle(
                "",
                new Delivery(new Envelope(1L, false, EXCHANGE, ROUTING_KEY), null, eventAsBytes));

        assertThat(handler.isEventReceived(), is(true));
        assertThat(handler.getPayload(), is(PAYLOAD));
    }

    private MethodHandle createHandle() throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType methodType = MethodType.methodType(void.class, MyTestEvent.class);
        return publicLookup.findVirtual(TestEventHandler.class, TEST_METHOD_NAME, methodType);
    }

    public static class TestEventHandler {
        private boolean eventReceived = false;
        private String payload;

        public void testMethod(MyTestEvent event) {
            eventReceived = true;
            payload = event.getPayload();
        }

        public boolean isEventReceived() {
            return eventReceived;
        }

        public String getPayload() {
            return payload;
        }
    }

    public static class MyTestEvent {
        private String payload;

        public MyTestEvent() {
        }

        public MyTestEvent(String payload) {
            this.payload = payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }
}
