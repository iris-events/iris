package io.smallrye.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.databind.JsonNode;

public class TestEventV2 {

    private int id;
    private String name;
    private String surname;
    private User user;
    private JsonNode payload;

    public TestEventV2(int id, String name, String surname, User user, JsonNode payload) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.user = user;
        this.payload = payload;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public User getUser() {
        return user;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
