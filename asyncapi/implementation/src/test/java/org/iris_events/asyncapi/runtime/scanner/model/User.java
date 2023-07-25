package org.iris_events.asyncapi.runtime.scanner.model;

import org.iris_events.asyncapi.spec.annotations.media.Schema;
import org.iris_events.asyncapi.spec.annotations.media.SchemaProperty;

@Schema(description = "This is a User schema component")
public class User {
    @SchemaProperty(description = "Name of the user")
    private String name;
    @SchemaProperty(description = "Surname of the user")
    private String surname;
    @SchemaProperty(description = "Age of the user")
    private long age;
    @SchemaProperty(description = "Status of the user")
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
