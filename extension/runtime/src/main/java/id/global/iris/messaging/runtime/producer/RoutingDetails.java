package id.global.iris.messaging.runtime.producer;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;

public record RoutingDetails(String eventName,
        String exchange,
        ExchangeType exchangeType,
        String routingKey,
        Scope scope,
        String userId,
        String sessionId,
        String subscriptionId) {
}
