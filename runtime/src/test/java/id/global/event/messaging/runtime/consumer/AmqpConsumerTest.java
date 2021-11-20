package id.global.event.messaging.runtime.consumer;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.channel.ConsumerChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.connection.ConnectionFactoryProvider;
import id.global.event.messaging.runtime.connection.ConsumerConnectionProvider;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;

public class AmqpConsumerTest {

    public static final String PAYLOAD = "testPayload";
    public static final String EXCHANGE = "testExchange";
    public static final String ROUTING_KEY = "MyTestEvent";
    public static final String TEST_METHOD_NAME = "testMethod";
    @Inject
    HostnameProvider hostnameProvider;

    @Test
    void consumerMethodHandleShouldCorrectlyInvoke() throws NoSuchMethodException, IllegalAccessException, IOException {
        TestEventHandler handler = new TestEventHandler();

        AmqpConfiguration amqpConfiguration = new AmqpConfiguration();
        AmqpConsumer consumer = new AmqpConsumer(
                new ObjectMapper(),
                createHandle(),
                new MethodHandleContext(TestEventHandler.class, MyTestEvent.class, void.class, TEST_METHOD_NAME),
                new AmqpContext(EXCHANGE, new String[0], DIRECT, Scope.INTERNAL, false, false, false, 1, -1, ""),
                new ConsumerChannelService(
                        new ConsumerConnectionProvider(
                                new ConnectionFactoryProvider(amqpConfiguration),
                                new HostnameProvider(),
                                amqpConfiguration),
                        amqpConfiguration),
                handler,
                new EventContext(),
                null, hostnameProvider, "test-app");

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

        @SuppressWarnings("unused")
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

        @SuppressWarnings("unused")
        public MyTestEvent() {
        }

        public MyTestEvent(String payload) {
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }
}
