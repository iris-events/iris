package id.global.event.messaging.test;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;

import id.global.common.annotations.amqp.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue")
public record Event(String name, Long age) {
}
