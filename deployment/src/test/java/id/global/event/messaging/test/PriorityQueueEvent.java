package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;

@ConsumedEvent(queue = "event-queue-priority")
public record PriorityQueueEvent(String name, Long age) {
}
