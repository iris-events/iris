package id.global.iris.messaging.runtime.producer;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;

public record RoutingDetails(String exchange, ExchangeType exchangeType, String routingKey, Scope scope, String userId) {
}
