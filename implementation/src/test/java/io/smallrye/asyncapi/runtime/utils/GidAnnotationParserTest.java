package io.smallrye.asyncapi.runtime.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.jboss.jandex.DotName;
import org.junit.Test;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.ExchangeType;
import io.smallrye.asyncapi.runtime.util.GidAnnotationParser;

public class GidAnnotationParserTest {

    @Test
    public void annotationParserShouldReturnCorrectExchangeType() {
        String directMsgHandlerName = MessageHandler.class.getName();
        String directMsgHandlerSimpleName = MessageHandler.class.getSimpleName();
        DotName directMessageHandler = DotName.createSimple(directMsgHandlerName);
        DotName directMessageHandlerSimple = DotName.createSimple(directMsgHandlerSimpleName);
        ExchangeType directExchange = GidAnnotationParser.getExchangeTypeFromAnnotation(directMessageHandler);
        ExchangeType directExchangeFromSimple = GidAnnotationParser.getExchangeTypeFromAnnotation(directMessageHandlerSimple);

        String topicMsgHandlerName = TopicMessageHandler.class.getName();
        String topicMsgHandlerSimpleName = TopicMessageHandler.class.getSimpleName();
        DotName topicMessageHandler = DotName.createSimple(topicMsgHandlerName);
        DotName topicMessageHandlerSimple = DotName.createSimple(topicMsgHandlerSimpleName);

        ExchangeType topicExchange = GidAnnotationParser.getExchangeTypeFromAnnotation(topicMessageHandler);
        ExchangeType topicExchangeFromSimple = GidAnnotationParser.getExchangeTypeFromAnnotation(topicMessageHandlerSimple);

        String fanoutMsgHandlerName = FanoutMessageHandler.class.getName();
        String fanoutMsgHandlerSimpleName = FanoutMessageHandler.class.getSimpleName();
        DotName fanoutMessageHandler = DotName.createSimple(fanoutMsgHandlerName);
        DotName fanoutMessageHandlerSimple = DotName.createSimple(fanoutMsgHandlerSimpleName);

        ExchangeType fanoutExchange = GidAnnotationParser.getExchangeTypeFromAnnotation(fanoutMessageHandler);
        ExchangeType fanoutExchangeFromSimple = GidAnnotationParser.getExchangeTypeFromAnnotation(fanoutMessageHandlerSimple);

        assertEquals(ExchangeType.DIRECT, directExchange);
        assertEquals(ExchangeType.TOPIC, topicExchange);
        assertEquals(ExchangeType.FANOUT, fanoutExchange);
        assertEquals(ExchangeType.DIRECT, directExchangeFromSimple);
        assertEquals(ExchangeType.TOPIC, topicExchangeFromSimple);
        assertEquals(ExchangeType.FANOUT, fanoutExchangeFromSimple);
    }

    @Test
    public void annotationParserShouldThrowExceptionOnUnknownHandlerAnnotation() {
        String unknownMessageHandlerName = "NewMessageHandler";
        DotName unknownMessageHandlerDotName = DotName.createSimple(unknownMessageHandlerName);

        assertThrows(IllegalArgumentException.class, () -> {
            GidAnnotationParser.getExchangeTypeFromAnnotation(unknownMessageHandlerDotName);
        });
    }
}
