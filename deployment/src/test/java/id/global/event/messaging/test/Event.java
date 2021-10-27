package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;

@ConsumedEvent(queue = "event-queue")
public record Event(String name, Long age) {
}
