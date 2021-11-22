package id.global.asyncapi.runtime.scanner.model;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.Scope.SESSION;

import id.global.common.annotations.amqp.Message;

@Message(
        name = "sent-event-exchange",
        routingKey = "sent-event-queue",
        scope = SESSION,
        exchangeType = DIRECT,
        rolesAllowed = { "ADMIN", "USER", "DUMMY" })
public class SentEvent {
    private int id;
    private String status;
    private User user;

    public SentEvent(int id, String status, User user) {
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
