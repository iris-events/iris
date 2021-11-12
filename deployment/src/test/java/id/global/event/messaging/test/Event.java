package id.global.event.messaging.test;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;

import id.global.common.annotations.amqp.ConsumedEvent;

@ConsumedEvent(exchange = "exchange", exchangeType = DIRECT, bindingKeys = "event-queue")
public record Event(String name, Long age) {
}
