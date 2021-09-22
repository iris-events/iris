package id.global.models.test;

import io.smallrye.asyncapi.runtime.scanner.model.User;

public class SimulatedGeneratedEvent {
    private int id;
    private String status;
    private User user;

    public SimulatedGeneratedEvent(int id, String status, User user) {
        this.id = id;
        this.status = status;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }
}
