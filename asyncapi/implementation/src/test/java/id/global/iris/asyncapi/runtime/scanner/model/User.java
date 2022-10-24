package id.global.iris.asyncapi.runtime.scanner.model;

import id.global.iris.asyncapi.spec.annotations.media.Schema;
import id.global.iris.asyncapi.spec.annotations.media.SchemaProperty;

@Schema(description="This is a User schema component")
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
