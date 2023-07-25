package org.iris_events.asyncapi.runtime.util;

import java.util.Set;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.asyncapi.runtime.io.channel.operation.OperationConstant;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelInfo;
import org.iris_events.asyncapi.runtime.scanner.model.OperationBindingsInfo;
import org.jboss.jandex.Type;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(
            final String exchange,
            final String bindingKeysCsv,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final Set<String> rolesAllowed,
            final String deadLetterQueue,
            final Integer ttl,
            final boolean persistent) {

        final var operationBindingsInfo = new OperationBindingsInfo(persistent);
        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, bindingKeysCsv, exchangeType);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, operationBindingsInfo,
                OperationConstant.PROP_SUBSCRIBE, rolesAllowed, deadLetterQueue, ttl, null);
    }

    public static ChannelInfo generatePublishChannelInfo(
            final String exchange,
            final String routingKey,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final Set<String> rolesAllowed,
            final String deadLetterQueue,
            final Integer ttl,
            final Type responseType,
            final boolean persistent) {

        final var operationBindingsInfo = new OperationBindingsInfo(persistent);
        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, routingKey, exchangeType, durable, autodelete);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, operationBindingsInfo, OperationConstant.PROP_PUBLISH,
                rolesAllowed, deadLetterQueue, ttl, responseType);
    }
}
