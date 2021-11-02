package id.global.event.messaging.runtime.connection;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.exception.AmqpConnectionFactoryException;

@ApplicationScoped
public class ConnectionFactoryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

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

        LOG.info(String.format("AMQP configuration: host=%s, port=%s, username=%s, ssl=%s",
                amqpConfiguration.getUrl(), port, amqpConfiguration.getUsername(), amqpConfiguration.isSslEnabled()));

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();

            connectionFactory.setUsername(amqpConfiguration.getUsername());
            connectionFactory.setPassword((amqpConfiguration.getPassword()));
            connectionFactory.setHost(amqpConfiguration.getUrl());
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);
            if (amqpConfiguration.isSslEnabled()) {
                connectionFactory.useSslProtocol();
            }

            return connectionFactory;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Could not create AMQP ConnectionFactory!", e);
            throw new AmqpConnectionFactoryException("Could not create AMQP ConnectionFactory", e);
        }
    }
}
