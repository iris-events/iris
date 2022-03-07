package id.global.event.messaging.runtime.health;

import java.util.concurrent.atomic.AtomicBoolean;

public record HealthStatus(AtomicBoolean isHealthy, AtomicBoolean connecting, AtomicBoolean timedOut) {
}
