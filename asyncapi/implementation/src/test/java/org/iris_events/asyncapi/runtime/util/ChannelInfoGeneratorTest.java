package org.iris_events.asyncapi.runtime.util;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelInfo;
import org.iris_events.asyncapi.runtime.scanner.model.OperationBindingsInfo;
import org.junit.jupiter.api.Test;

public class ChannelInfoGeneratorTest {

    @Test
    public void channelInfoGeneratorShouldGenerateCorrectInfoAndBindings() {
        final var exchange = "testExchange";
        final var queue = "testQueue";
        final var eventClass = "EventClassName";
        final var deadLetterQueue = "dead-letter";
        final var ttl = -1;
        final var exchangeType = ExchangeType.DIRECT;
        final var persistentMessage = false;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchange, queue, eventClass, exchangeType, emptySet(), deadLetterQueue, ttl,
                        persistentMessage);

        assertNotNull(channelInfo);
        assertEquals(eventClass, channelInfo.getEventKey());
        assertNotNull(channelInfo.getBindingsInfo());

        ChannelBindingsInfo bindingsInfo = channelInfo.getBindingsInfo();
        assertEquals(exchange, bindingsInfo.getExchange());
        assertEquals(exchangeType, bindingsInfo.getExchangeType());
        assertEquals(queue, bindingsInfo.getQueue());
        assertTrue(bindingsInfo.isExchangeDurable());
        assertFalse(bindingsInfo.isExchangeAutoDelete());

        OperationBindingsInfo operationBindingsInfo = channelInfo.getOperationBindingsInfo();
        assertFalse(operationBindingsInfo.persistent());

        assertThat(bindingsInfo.isQueueAutoDelete(), is(nullValue()));
        assertThat(bindingsInfo.isQueueDurable(), is(nullValue()));

        assertEquals("/", bindingsInfo.getExchangeVhost());
        assertEquals("/", bindingsInfo.getQueueVhost());
    }

    @Test
    public void channelInfoGeneratorShouldUseEventNameIfQueueMissing() {
        final var exchange = "testExchange";
        final var eventClass = "EventClassName";
        final var queueName = "event-class-name";
        final var deadLetterQueue = "dead-letter";
        final var ttl = -1;
        final var persistentMessage = false;
        final var exchangeType = ExchangeType.DIRECT;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchange, queueName, eventClass, exchangeType, emptySet(), deadLetterQueue,
                        ttl, persistentMessage);

        assertThat(channelInfo.getBindingsInfo().getQueue(), is(queueName));
        assertThat(channelInfo.getEventKey(), is(eventClass));
    }
}
