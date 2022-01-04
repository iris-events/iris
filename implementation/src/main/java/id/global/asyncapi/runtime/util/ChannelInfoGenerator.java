package id.global.asyncapi.runtime.util;

import java.util.Set;

import id.global.asyncapi.runtime.io.channel.operation.OperationConstant;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import id.global.asyncapi.runtime.scanner.model.ChannelInfo;
import id.global.common.auth.jwt.Role;

public class ChannelInfoGenerator {

    public static ChannelInfo generateSubscribeChannelInfo(
            final String exchange,
            final String bindingKeysCsv,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final Set<Role> rolesAllowed,
            final String deadLetterQueue,
            final Integer ttl) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, bindingKeysCsv, exchangeType);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, OperationConstant.PROP_SUBSCRIBE, rolesAllowed, deadLetterQueue, ttl);
    }

    public static ChannelInfo generatePublishChannelInfo(
            final String exchange,
            final String routingKey,
            final String eventClassSimpleName,
            final ExchangeType exchangeType,
            final boolean durable,
            final boolean autodelete,
            final Set<Role> rolesAllowed,
            final String deadLetterQueue,
            final Integer ttl) {

        final var channelBindingsInfo = new ChannelBindingsInfo(exchange, routingKey, exchangeType, durable, autodelete);
        return new ChannelInfo(eventClassSimpleName, channelBindingsInfo, OperationConstant.PROP_PUBLISH, rolesAllowed, deadLetterQueue, ttl);
    }
}
