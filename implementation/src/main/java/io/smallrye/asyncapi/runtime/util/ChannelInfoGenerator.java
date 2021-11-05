package io.smallrye.asyncapi.runtime.util;

import id.global.common.annotations.amqp.ExchangeType;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(
            final String exchange,
            final String queue,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final String[] rolesAllowed) {

        return generateChannelInfo(exchange, queue, eventClassSimpleName, exchangeType, rolesAllowed,
                OperationConstant.PROP_SUBSCRIBE);
    }

    public static ChannelInfo generatePublishChannelInfo(
            final String exchange,
            final String queue,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final String[] rolesAllowed) {

        return generateChannelInfo(exchange, queue, eventClassSimpleName, exchangeType, rolesAllowed,
                OperationConstant.PROP_PUBLISH);
    }

    public static ChannelInfo generateChannelInfo(
            final String exchange,
            final String queue,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final String[] rolesAllowed,
            final String operationType) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, queue, exchangeType);

        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, operationType, rolesAllowed);
    }
}
