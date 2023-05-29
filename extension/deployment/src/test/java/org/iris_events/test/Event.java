package org.iris_events.test;

import static org.iris_events.annotations.ExchangeType.DIRECT;

import org.iris_events.annotations.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue")
public record Event(String name, Long age) {
}
