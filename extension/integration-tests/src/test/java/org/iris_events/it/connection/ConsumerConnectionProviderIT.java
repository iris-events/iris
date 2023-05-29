package org.iris_events.it.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.iris_events.runtime.connection.ConsumerConnectionProvider;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Connection;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConsumerConnectionProviderIT {

    @Inject
    Instance<ConsumerConnectionProvider> provider;

    @Test
    public void provideConnection() {
        Connection connection = provider.get().getConnection();
        assertThat(connection, is(notNullValue()));
    }
}
