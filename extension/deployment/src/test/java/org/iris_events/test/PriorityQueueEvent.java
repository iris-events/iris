package org.iris_events.test;

import static org.iris_events.annotations.ExchangeType.DIRECT;

import org.iris_events.annotations.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
