package io.smallrye.asyncapi.runtime.scanner.model;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.asyncapi.spec.enums.Scope;

@ProducedEvent(
        exchange = "sentEventExchange",
        queue = "sentEventQueue",
        scope = Scope.EXTERNAL,
        exchangeType = ExchangeType.DIRECT,
        rolesAllowed = { "ADMIN", "USER", "DUMMY" })
@ConsumedEvent(queue = "sentEventV1")
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
