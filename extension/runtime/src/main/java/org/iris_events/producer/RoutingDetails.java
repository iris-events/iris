package org.iris_events.producer;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;

public record RoutingDetails(String eventName,
                             String exchange,
                             ExchangeType exchangeType,
                             String routingKey,
                             Scope scope,
                             String userId,
                             String sessionId,
                             String subscriptionId,
                             boolean persistent,
                             Integer cacheTtl) {
}
