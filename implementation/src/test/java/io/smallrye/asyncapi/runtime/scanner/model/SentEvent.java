package io.smallrye.asyncapi.runtime.scanner.model;

import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.asyncapi.spec.enums.EventType;
import id.global.asyncapi.spec.enums.ExchangeType;

@ProducedEvent(
        exchange = "sentEventExchange",
        queue = "sentEventQueue",
        eventType = EventType.EXTERNAL,
        exchangeType = ExchangeType.DIRECT,
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
