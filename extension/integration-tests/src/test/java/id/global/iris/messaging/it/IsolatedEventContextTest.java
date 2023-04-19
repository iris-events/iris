package id.global.iris.messaging.it;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import id.global.iris.messaging.runtime.context.EventContext;
import io.quarkus.test.junit.QuarkusMock;

public abstract class IsolatedEventContextTest {

    @Inject
    public EventContext eventContext;

    @BeforeEach
    void setUp() {
        // since all tests are started from main thread,
        // it is important to use custom event context instance when relying on event context across multiple threads (event handlers)
        QuarkusMock.installMockForInstance(new EventContextMock(), eventContext);
    }

    public static class EventContextMock extends EventContext {
    }
}
