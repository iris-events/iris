package org.iris_events.producer;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CorrelationIdProvider {
    public String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
