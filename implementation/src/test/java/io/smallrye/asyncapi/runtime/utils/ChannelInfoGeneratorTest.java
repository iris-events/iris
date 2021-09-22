package io.smallrye.asyncapi.runtime.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.jandex.AnnotationValue;
import org.junit.Test;

import id.global.asyncapi.spec.enums.ExchangeType;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;
import io.smallrye.asyncapi.runtime.util.ChannelInfoGenerator;

public class ChannelInfoGeneratorTest {

    @Test
    public void channelInfoGeneratorShouldGenerateCorrectInfoAndBindings() {
        String exchange = "testExchange";
        AnnotationValue exchangeValue = AnnotationValue.createStringValue("exchange", exchange);
        String queue = "testQueue";
        AnnotationValue queueValue = AnnotationValue.createStringValue("queue", queue);
        String eventClass = "EventClassName";
        ExchangeType exchangeType = ExchangeType.DIRECT;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchangeValue, queueValue, eventClass, exchangeType, new String[0]);

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
        AnnotationValue exchangeValue = AnnotationValue.createStringValue("exchange", exchange);
        String eventClass = "EventClassName";
        ExchangeType exchangeType = ExchangeType.DIRECT;

        ChannelInfo channelInfo = ChannelInfoGenerator
                .generateSubscribeChannelInfo(exchangeValue, null, eventClass, exchangeType, new String[0]);

        assertEquals(eventClass, channelInfo.getBindingsInfo().getQueue());
    }

}
