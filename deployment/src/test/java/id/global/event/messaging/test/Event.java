package id.global.event.messaging.test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;

@ConsumedEvent(queue = "event-queue")
public class Event {

    private final String name;
    private final Long age;

    public Event(String name, Long age) {
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
