package io.smallrye.asyncapi.runtime.util;

import static io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant.PROP_PUBLISH;
import static io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant.PROP_SUBSCRIBE;

import id.global.common.annotations.amqp.ExchangeType;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(
            final String exchange,
            final String bindingKeysCsv,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final String[] rolesAllowed,
            final String deadLetterQueue,
            final int ttl) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, bindingKeysCsv, exchangeType);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, PROP_SUBSCRIBE, rolesAllowed, deadLetterQueue, ttl);
    }

    public static ChannelInfo generatePublishChannelInfo(
            final String exchange,
            final String routingKey,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final String[] rolesAllowed,
            final String deadLetterQueue,
            final int ttl) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, routingKey, exchangeType, durable, autodelete);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, PROP_PUBLISH, rolesAllowed, deadLetterQueue, ttl);
    }
}
