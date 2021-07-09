package id.global.event.messaging.it.events;

public class Event {

    private String name;
    private Long age;

    public Event() {
    }

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

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Long age) {
        this.age = age;
    }
}