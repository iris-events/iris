package id.global.iris.messaging.runtime;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TimestampProvider {
    public long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }
}
