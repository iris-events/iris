package id.global.event.messaging.runtime.health;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
@Default
public class IrisHealthCheck implements HealthCheck {
    private final HealthStatus healthStatus;

    public IrisHealthCheck() {
        this.healthStatus = new HealthStatus(
                new AtomicBoolean(false),
                new AtomicBoolean(false),
                new AtomicBoolean(false));
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("IRIS amqp connection health check")
                .withData("connecting", healthStatus.connecting().get())
                .withData("timedOut", healthStatus.timedOut().get())
                .up();
        if (healthStatus.isHealthy().get()) {
            return builder.build();
        }
        return builder.down().build();
    }

    public void setConnecting(boolean connecting) {
        this.healthStatus.connecting().set(connecting);
        this.healthStatus.isHealthy().set(getHealth(healthStatus));
    }

    public void setTimedOut(boolean timedOut) {
        this.healthStatus.timedOut().set(timedOut);
        this.healthStatus.isHealthy().set(getHealth(healthStatus));
    }

    private boolean getHealth(HealthStatus healthStatus) {
        return !healthStatus.timedOut().get() && !healthStatus.connecting().get();
    }
}
