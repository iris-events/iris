package id.global.event.messaging.runtime;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@Singleton
public class ConnectionFactoryProvider {

    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

    @Inject
    AmqpConfiguration amqpConfiguration;

    private ConnectionFactory connectionFactory;

    public ConnectionFactory getConnectionFactory() {
        return Optional.ofNullable(connectionFactory).orElseGet(() -> {
            final var connectionFactory = buildConnection(amqpConfiguration);
            this.connectionFactory = connectionFactory;
            return connectionFactory;
        });
    }

    private ConnectionFactory buildConnection(AmqpConfiguration amqpConfiguration) {
        ConnectionFactory factory = new ConnectionFactory();

        int port = amqpConfiguration.getPort();
        String protocol = amqpConfiguration.isSslEnabled() ? "amqps" : "amqp";

        log.info(String.format("AMQP configuration: protocol=%s, url=%s, port=%s, username=%s, ssl=%s", protocol,
                amqpConfiguration.getUrl(), port, amqpConfiguration.getUsername(), amqpConfiguration.isSslEnabled()));

        try {
            if (amqpConfiguration.isAuthenticated()) {
                String connectionUrl = String.format("%s://%s:%s@%s:%s%s",
                        protocol,
                        amqpConfiguration.getUsername(),
                        amqpConfiguration.getPassword(),
                        amqpConfiguration.getUrl(),
                        port,
                        "/%2f");

                log.info("Setting factory URL = " + connectionUrl);
                factory.setUri(connectionUrl);

            } else {
                String connectionUrl = String.format("%s://%s:%s%s",
                        protocol,
                        amqpConfiguration.getUrl(),
                        port,
                        "/%2f");

                log.info("Setting factory URL = " + connectionUrl);
                factory.setUri(connectionUrl);
            }
            factory.setAutomaticRecoveryEnabled(true);

            return factory;
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Cannot create ConnectionFactory!", e);
            return null;
        }

    }
}
