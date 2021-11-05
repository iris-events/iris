package io.smallrye.asyncapi.runtime.scanner.model;

import javax.annotation.Generated;

import id.global.common.annotations.amqp.ConsumedEvent;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.ProducedEvent;
import id.global.common.annotations.amqp.Scope;

@ProducedEvent(
        exchange = "sent-event-exchange",
        routingKey = "sent-event-queue",
        scope = Scope.EXTERNAL,
        exchangeType = ExchangeType.DIRECT,
        rolesAllowed = { "ADMIN", "USER", "DUMMY" })
@ConsumedEvent(routingKey = "sent-event-v1")
@Generated("")
@javax.annotation.processing.Generated("")
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
