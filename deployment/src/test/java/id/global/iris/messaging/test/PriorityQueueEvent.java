package id.global.iris.messaging.test;

import static id.global.common.annotations.iris.ExchangeType.DIRECT;

import id.global.common.annotations.iris.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
