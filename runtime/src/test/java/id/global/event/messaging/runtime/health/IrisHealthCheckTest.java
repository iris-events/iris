package id.global.event.messaging.runtime.health;

import java.util.stream.Stream;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IrisHealthCheckTest {

    private IrisHealthCheck healthCheck;

    @BeforeEach
    public void setup() {
        this.healthCheck = new IrisHealthCheck();
    }

    @ParameterizedTest
    @MethodSource
    public void call(boolean connecting, boolean timedOut, HealthCheckResponse.Status status) {
        this.healthCheck.setConnecting(connecting);
        this.healthCheck.setTimedOut(timedOut);

        HealthCheckResponse call = this.healthCheck.call();
        MatcherAssert.assertThat(call.getStatus(), CoreMatchers.is(status));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> call() {
        return Stream.of(
                Arguments.of(false, false, HealthCheckResponse.Status.UP),
                Arguments.of(true, false, HealthCheckResponse.Status.DOWN),
                Arguments.of(false, true, HealthCheckResponse.Status.DOWN),
                Arguments.of(true, true, HealthCheckResponse.Status.DOWN));
    }
}