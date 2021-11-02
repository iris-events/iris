package io.smallrye.asyncapi.runtime.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import id.global.common.annotations.amqp.ExchangeType;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;
import io.smallrye.asyncapi.runtime.util.ChannelInfoGenerator;

public class ChannelInfoGeneratorTest {

    @Test
    public void channelInfoGeneratorShouldGenerateCorrectInfoAndBindings() {
        String exchange = "testExchange";
        String queue = "testQueue";
        String eventClass = "EventClassName";
        ExchangeType exchangeType = ExchangeType.DIRECT;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchange, queue, eventClass, exchangeType, new String[0]);

        assertNotNull(channelInfo);
        assertEquals(eventClass, channelInfo.getEventKey());
        assertNotNull(channelInfo.getBindingsInfo());

        ChannelBindingsInfo bindingsInfo = channelInfo.getBindingsInfo();
        assertEquals(exchange, bindingsInfo.getExchange());
        assertEquals(exchangeType, bindingsInfo.getExchangeType());
        assertEquals(queue, bindingsInfo.getQueue());
        assertTrue(bindingsInfo.isExchangeDurable());
        assertFalse(bindingsInfo.isExchangeAutoDelete());
        assertFalse(bindingsInfo.isQueueAutoDelete());
        assertTrue(bindingsInfo.isQueueDurable());
        assertEquals("/", bindingsInfo.getExchangeVhost());
        assertEquals("/", bindingsInfo.getQueueVhost());
    }

    @Test
    public void channelInfoGeneratorShouldUseEventNameIfQueueMissing() {
        String exchange = "testExchange";
        String eventClass = "EventClassName";
        String queueName = "event-class-name";
        ExchangeType exchangeType = ExchangeType.DIRECT;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchange, queueName, eventClass, exchangeType, new String[0]);

        assertThat(channelInfo.getBindingsInfo().getQueue(), is(queueName));
        assertThat(channelInfo.getEventKey(), is(eventClass));
    }
}
