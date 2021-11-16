package io.smallrye.asyncapi.runtime.scanner.model;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.Scope.EXTERNAL;

import id.global.common.annotations.amqp.ConsumedEvent;
import id.global.common.annotations.amqp.ProducedEvent;

@ProducedEvent(
        exchange = "sent-event-exchange",
        routingKey = "sent-event-queue",
        scope = EXTERNAL,
        exchangeType = DIRECT,
        rolesAllowed = { "ADMIN", "USER", "DUMMY" })
@ConsumedEvent(bindingKeys = "sent-event-v1", exchangeType = DIRECT)
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
