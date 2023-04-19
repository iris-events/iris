package id.global.iris.messaging.it.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Connection;

import id.global.iris.messaging.runtime.connection.ConsumerConnectionProvider;
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
