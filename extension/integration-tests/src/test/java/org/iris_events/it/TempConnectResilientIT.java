package org.iris_events.it;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.Connection;

import org.iris_events.runtime.connection.ConsumerConnectionProvider;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TempConnectResilientIT extends IsolatedEventContextTest {

    @Inject
    ConsumerConnectionProvider testConnectionProvider;

    @Test
    public void testResilientConnection() {
        Connection connection = testConnectionProvider.getConnection();
        MatcherAssert.assertThat(connection, CoreMatchers.notNullValue());
    }
}
