package io.smallrye.asyncapi.runtime.util;

import org.jboss.jandex.AnnotationValue;

import id.global.asyncapi.spec.enums.ExchangeType;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(AnnotationValue exchange, AnnotationValue queue,
            String eventClassSimpleName, ExchangeType exchangeType, String[] rolesAllowed) {
        String exchangeValue = exchange != null ? exchange.asString() : "";
        String queueValue = queue != null ? queue.asString() : eventClassSimpleName;

        ChannelBindingsInfo channelBindingsInfo = new ChannelBindingsInfo(exchangeValue, queueValue, exchangeType);

        // Maybe we'll add publish channels in the future
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, OperationConstant.PROP_SUBSCRIBE, rolesAllowed);
    }

    public static ChannelInfo generatePublishChannelInfo(AnnotationValue exchange, AnnotationValue queue,
            String eventClassSimpleName, ExchangeType exchangeType, String[] rolesAllowed) {
        String exchangeValue = exchange != null ? exchange.asString() : "";
        String queueValue = queue != null ? queue.asString() : eventClassSimpleName;

        ChannelBindingsInfo channelBindingsInfo = new ChannelBindingsInfo(exchangeValue, queueValue, exchangeType);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, OperationConstant.PROP_PUBLISH, rolesAllowed);
    }
}
