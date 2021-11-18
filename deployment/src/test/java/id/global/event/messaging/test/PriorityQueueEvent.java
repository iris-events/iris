package id.global.event.messaging.test;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;

import id.global.common.annotations.amqp.Message;

@Message(exchange = "exchange", exchangeType = DIRECT, routingKey = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
