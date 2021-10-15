package id.global.event.messaging.it.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Connection;

import id.global.event.messaging.runtime.connection.ConsumerConnectionProvider;
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
