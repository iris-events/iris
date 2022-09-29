package id.global.iris.messaging.runtime.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.rabbitmq.RabbitMQOptions;

class ConnectionFactoryProviderTest {

    @Test
    void shouldSetAllProperties() {
        final RabbitMQOptions irisConfiguration = new RabbitMQOptions();

        final var username = "username.mock";
        final var password = "password.mock";
        final var url = "localhost.mock";
        final var port = 1234;
        final var sslEnabled = true;

        irisConfiguration.setUser(username);
        irisConfiguration.setPassword(password);
        irisConfiguration.setHost(url);
        irisConfiguration.setPort(port);
        irisConfiguration.setSsl(sslEnabled);

        final var factoryProvider = new ConnectionFactoryProvider(irisConfiguration);
        final var connectionFactory = factoryProvider.getConnectionFactory();

        assertThat(connectionFactory, notNullValue());
        assertThat(connectionFactory.getUsername(), is(username));
        assertThat(connectionFactory.getPassword(), is(password));
        assertThat(connectionFactory.getHost(), is(url));
        assertThat(connectionFactory.getPort(), is(port));
        assertThat(connectionFactory.isSSL(), is(sslEnabled));
    }

    @Test
    void shouldUrlEncodeUsernameAndPassword() {
        final RabbitMQOptions irisConfiguration = new RabbitMQOptions();

        final var username = "u$ern@me.m//ck";
        final var password = "p@$$word.m//ck";
        final var port = 5432;

        irisConfiguration.setUser(username);
        irisConfiguration.setPassword(password);
        irisConfiguration.setPort(port); // port is required here as mocked class does not provide default values

        final var factoryProvider = new ConnectionFactoryProvider(irisConfiguration);
        final var connectionFactory = factoryProvider.getConnectionFactory();

        assertThat(connectionFactory.getUsername(), is(username));
        assertThat(connectionFactory.getPassword(), is(password));
    }
}