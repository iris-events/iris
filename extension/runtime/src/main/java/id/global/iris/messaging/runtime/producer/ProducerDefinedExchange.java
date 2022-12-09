package id.global.iris.messaging.runtime.producer;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;

public record ProducerDefinedExchange(String exchangeName, ExchangeType type, Scope scope) {
}
