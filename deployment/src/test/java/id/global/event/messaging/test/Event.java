package id.global.event.messaging.test;

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
