package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.enums.ExchangeType;

@ConsumedEvent(exchange = "exchange", exchangeType = ExchangeType.DIRECT, queue = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
