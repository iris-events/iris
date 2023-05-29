package org.iris_events.health;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

/**
 * Iris service is live, when amqp connection is ready or connecting (connecting = true/false) and is not timed out (timedOut =
 * false).
 */
@Liveness
@ApplicationScoped
@Default
public class IrisLivenessCheck implements HealthCheck {

    public record LivenessStatus(AtomicBoolean isHealthy, AtomicBoolean timedOut) {
    }

    private final LivenessStatus livenessStatus;

    public IrisLivenessCheck() {
        this.livenessStatus = new LivenessStatus(
                new AtomicBoolean(false),
                new AtomicBoolean(false));
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Iris amqp connection health check")
                .withData("timedOut", livenessStatus.timedOut().get())
                .up();
        if (livenessStatus.isHealthy().get()) {
            return builder.build();
        }

        return builder.down().build();
    }

    public void setTimedOut(boolean timedOut) {
        this.livenessStatus.timedOut().set(timedOut);
        this.livenessStatus.isHealthy().set(getHealth(livenessStatus));
    }

    private boolean getHealth(LivenessStatus livenessStatus) {
        return !livenessStatus.timedOut().get();
    }
}
