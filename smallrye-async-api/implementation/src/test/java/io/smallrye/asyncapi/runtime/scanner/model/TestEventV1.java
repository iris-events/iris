package io.smallrye.asyncapi.runtime.scanner.model;

public class TestEventV1 {

    private int id;
    private String name;
    private User user;

    public TestEventV1(int id, String name, User user) {
        this.id = id;
        this.name = name;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }
}
