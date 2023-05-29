package org.iris_events.runtime;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TimestampProvider {
    public long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }
}
