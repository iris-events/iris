package id.global.event.messaging.it;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.ExchangeType;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeFailIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test_EventQueue";
    private static final String UNKNOWN_EXCHANGE = "unknown_exchange";

    @Inject
    AmqpProducer producer;

    @Test
    void basicProduceUnknownExchangeTest() {

        boolean published = producer.publish(
                UNKNOWN_EXCHANGE,
                Optional.of(EVENT_QUEUE),
                ExchangeType.DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                null, true);

        assertFalse(published);
    }

}
