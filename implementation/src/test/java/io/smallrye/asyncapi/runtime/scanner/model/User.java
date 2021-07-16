package io.smallrye.asyncapi.runtime.scanner.model;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

@Schema(name = "user")
public class User {
    private String name;
    private String surname;
    private long age;
    private Status status;

    public User(String name, String surname, long age, Status status) {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public long getAge() {
        return age;
    }

    public Status getStatus() {
        return status;
    }
}
