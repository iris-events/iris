package id.global.event.messaging.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

class ConnectionFactoryProviderTest {

    @Test
    void shouldSetAllProperties() {
        final AmqpConfiguration amqpConfiguration = new AmqpConfiguration();

        final var username = "username.mock";
        final var password = "password.mock";
        final var url = "localhost.mock";
        final var port = 1234;
        final var sslEnabled = true;

        amqpConfiguration.setUsername(username);
        amqpConfiguration.setPassword(password);
        amqpConfiguration.setUrl(url);
        amqpConfiguration.setPort(port);
        amqpConfiguration.setSslEnabled(sslEnabled);

        final var factoryProvider = new ConnectionFactoryProvider(amqpConfiguration);
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
        final AmqpConfiguration amqpConfiguration = new AmqpConfiguration();

        final var username = "u$ern@me.m//ck";
        final var password = "p@$$word.m//ck";
        final var port = 5432;

        amqpConfiguration.setUsername(username);
        amqpConfiguration.setPassword(password);
        amqpConfiguration.setPort(port); // port is required here as mocked class does not provide default values

        final var factoryProvider = new ConnectionFactoryProvider(amqpConfiguration);
        final var connectionFactory = factoryProvider.getConnectionFactory();

        assertThat(connectionFactory.getUsername(), is(URLEncoder.encode(username, StandardCharsets.UTF_8)));
        assertThat(connectionFactory.getPassword(), is(URLEncoder.encode(password, StandardCharsets.UTF_8)));
    }
}