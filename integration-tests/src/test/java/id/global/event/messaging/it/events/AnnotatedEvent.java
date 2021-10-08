package id.global.event.messaging.it.events;

import id.global.common.annotations.EventMetadata;

@EventMetadata(exchange = "annotated-exchange", routingKey = "annotated-queue", exchangeType = "direct")
public class AnnotatedEvent {
    private String name;
    private Long age;

    public AnnotatedEvent() {
    }

    public AnnotatedEvent(String name, Long age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public Long getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Long age) {
        this.age = age;
    }
}
