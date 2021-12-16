package id.global.event.messaging.runtime.consumer;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.TestChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;
import id.global.event.messaging.runtime.requeue.RetryQueues;

public class AmqpConsumerTest {

    public static final String PAYLOAD = "testPayload";
    public static final String EXCHANGE = "testExchange";
    public static final String ROUTING_KEY = "MyTestEvent";
    public static final String TEST_METHOD_NAME = "testMethod";
    @Inject
    InstanceInfoProvider instanceInfoProvider;

    @Test
    void consumerMethodHandleShouldCorrectlyInvoke() throws NoSuchMethodException, IllegalAccessException, IOException {
        TestEventHandler handler = new TestEventHandler();

        TestChannelService channelService = new TestChannelService();
        RetryQueues testRetryQueues = getTestRetryQueues();
        AmqpConsumer consumer = new AmqpConsumer(
                new ObjectMapper(),
                createHandle(),
                new MethodHandleContext(TestEventHandler.class, MyTestEvent.class, null, TEST_METHOD_NAME),
                new AmqpContext(EXCHANGE, List.of(), DIRECT, Scope.INTERNAL, false, false, false, 1, -1, ""),
                channelService,
                handler,
                new EventContext(),
                null,
                instanceInfoProvider,
                new MessageRequeueHandler(channelService, testRetryQueues),
                testRetryQueues);

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

    private RetryQueues getTestRetryQueues() {
        AmqpConfiguration config = new AmqpConfiguration();

        config.setRetryFactor(3);
        config.setRetryInitialInterval(200);
        config.setRetryFactor(1.1);

        return new RetryQueues(config);
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
