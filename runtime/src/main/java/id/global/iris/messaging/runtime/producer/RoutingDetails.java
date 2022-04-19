package id.global.iris.messaging.runtime.producer;

import id.global.common.annotations.iris.ExchangeType;
import id.global.common.annotations.iris.Scope;

public record RoutingDetails(String eventName,
        String exchange,
        ExchangeType exchangeType,
        String routingKey,
        Scope scope,
        String userId,
        String sessionId,
        String subscriptionId) {
}
