package org.iris_events.it;

import jakarta.inject.Inject;

import org.iris_events.context.EventContext;
import org.junit.jupiter.api.BeforeEach;

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
