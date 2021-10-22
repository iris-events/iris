package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;

@ConsumedEvent(queue = "event-queue-priority")
public class PriorityQueueEvent {

    private final String name;
    private final Long age;

    public PriorityQueueEvent(String name, Long age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public Long getAge() {
        return age;
    }
}
