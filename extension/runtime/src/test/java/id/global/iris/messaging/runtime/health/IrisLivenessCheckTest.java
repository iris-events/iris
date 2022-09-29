package id.global.iris.messaging.runtime.health;

import java.util.stream.Stream;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IrisLivenessCheckTest {

    private IrisLivenessCheck healthCheck;

    @BeforeEach
    public void setup() {
        this.healthCheck = new IrisLivenessCheck();
    }

    @ParameterizedTest
    @MethodSource
    public void call(boolean timedOut, HealthCheckResponse.Status status) {
        this.healthCheck.setTimedOut(timedOut);

        HealthCheckResponse call = this.healthCheck.call();
        MatcherAssert.assertThat(call.getStatus(), CoreMatchers.is(status));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> call() {
        return Stream.of(
                Arguments.of(false, HealthCheckResponse.Status.UP),
                Arguments.of(true, HealthCheckResponse.Status.DOWN));
    }
}