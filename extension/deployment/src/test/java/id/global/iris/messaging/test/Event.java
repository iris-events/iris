package id.global.iris.messaging.test;

import static id.global.common.iris.annotations.ExchangeType.DIRECT;

import id.global.common.iris.annotations.Message;

@Message(name = "exchange", exchangeType = DIRECT, routingKey = "event-queue")
public record Event(String name, Long age) {
}