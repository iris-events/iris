package org.iris_events.producer;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;

public record ProducerDefinedExchange(String exchangeName, ExchangeType type, Scope scope) {
}
