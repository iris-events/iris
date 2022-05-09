package id.global.iris.messaging.test;

import static id.global.common.iris.annotations.ExchangeType.DIRECT;

import id.global.common.iris.annotations.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
