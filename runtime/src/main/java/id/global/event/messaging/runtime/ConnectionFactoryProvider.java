package id.global.event.messaging.runtime;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.exception.AmqpConnectionFactoryException;

@Singleton
public class ConnectionFactoryProvider {

    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

    AmqpConfiguration amqpConfiguration;

    private ConnectionFactory connectionFactory;

    @Inject
    public ConnectionFactoryProvider(AmqpConfiguration amqpConfiguration) {
        this.amqpConfiguration = amqpConfiguration;
    }

    public ConnectionFactory getConnectionFactory() {
        return Optional.ofNullable(connectionFactory).orElseGet(() -> {
            final var connectionFactory = buildConnectionFactory(amqpConfiguration);
            this.connectionFactory = connectionFactory;
            return connectionFactory;
        });
    }

    private ConnectionFactory buildConnectionFactory(AmqpConfiguration amqpConfiguration) {
        int port = amqpConfiguration.getPort();
        // TODO this should be a configurable property with / as default
        String vhost = "/";

        log.info(String.format("AMQP configuration: host=%s, port=%s, username=%s, ssl=%s",
                amqpConfiguration.getUrl(), port, amqpConfiguration.getUsername(), amqpConfiguration.isSslEnabled()));

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();

            connectionFactory.setUsername(URLEncoder.encode(amqpConfiguration.getUsername(), StandardCharsets.UTF_8));
            connectionFactory.setPassword(URLEncoder.encode(amqpConfiguration.getPassword(), StandardCharsets.UTF_8));
            connectionFactory.setHost(amqpConfiguration.getUrl());
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);
            if (amqpConfiguration.isSslEnabled()) {
                connectionFactory.useSslProtocol();
            }

            return connectionFactory;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Could not create AMQP ConnectionFactory!", e);
            throw new AmqpConnectionFactoryException("Could not create AMQP ConnectionFactory", e);
        }
    }
}
