package io.smallrye.asyncapi.runtime.util;

import id.global.common.annotations.amqp.ExchangeType;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(
            final String exchange,
            final String bindingKeysCsv,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final String[] rolesAllowed) {

        return generateChannelInfo(
                exchange,
                bindingKeysCsv,
                eventClassSimpleName,
                exchangeType,
                durable,
                autodelete,
                rolesAllowed,
                OperationConstant.PROP_SUBSCRIBE);
    }

    public static ChannelInfo generatePublishChannelInfo(
            final String exchange,
            final String routingKey,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final String[] rolesAllowed) {

        return generateChannelInfo(exchange, routingKey, eventClassSimpleName, exchangeType, durable, autodelete, rolesAllowed,
                OperationConstant.PROP_PUBLISH);
    }

    private static ChannelInfo generateChannelInfo(
            final String exchange,
            final String queueBinding,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final String[] rolesAllowed,
            final String operationType) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, queueBinding, exchangeType, durable, autodelete);

        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, operationType, rolesAllowed);
    }
}
