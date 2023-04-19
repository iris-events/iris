package id.global.iris.messaging.runtime.health;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Iris service is ready, when amqp connection is ready (connecting = false) and is not timed out (timedOut = false).
 */
@Readiness
@ApplicationScoped
@Default
public class IrisReadinessCheck implements HealthCheck {

    public record ReadinessStatus(AtomicBoolean isHealthy, AtomicBoolean connecting, AtomicBoolean timedOut) {
    }

    private final ReadinessStatus readinessStatus;

    public IrisReadinessCheck() {
        this.readinessStatus = new ReadinessStatus(
                new AtomicBoolean(false),
                new AtomicBoolean(false),
                new AtomicBoolean(false));
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Iris amqp connection readiness check")
                .withData("connecting", readinessStatus.connecting().get())
                .withData("timedOut", readinessStatus.timedOut().get())
                .up();
        if (readinessStatus.isHealthy().get()) {
            return builder.build();
        }
        return builder.down().build();
    }

    public void setConnecting(boolean connecting) {
        this.readinessStatus.connecting().set(connecting);
        this.readinessStatus.isHealthy().set(getHealth(readinessStatus));
    }

    public void setTimedOut(boolean timedOut) {
        this.readinessStatus.timedOut().set(timedOut);
        this.readinessStatus.isHealthy().set(getHealth(readinessStatus));
    }

    private boolean getHealth(ReadinessStatus readinessStatus) {
        return !readinessStatus.timedOut().get() && !readinessStatus.connecting().get();
    }
}
