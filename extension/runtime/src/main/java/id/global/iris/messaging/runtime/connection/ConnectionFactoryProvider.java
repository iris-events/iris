package id.global.iris.messaging.runtime.connection;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import id.global.iris.messaging.runtime.exception.IrisConnectionFactoryException;
import io.vertx.rabbitmq.RabbitMQOptions;

@ApplicationScoped
public class ConnectionFactoryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

    private final RabbitMQOptions rabbitMQOptions;

    private ConnectionFactory connectionFactory;

    @Inject
    public ConnectionFactoryProvider(@Named("IrisRabbitMQOptions") RabbitMQOptions rabbitMQOptions) {
        this.rabbitMQOptions = rabbitMQOptions;
    }

    public ConnectionFactory getConnectionFactory() {
        return Optional.ofNullable(connectionFactory).orElseGet(() -> {
            final var connectionFactory = buildConnectionFactory(rabbitMQOptions);
            this.connectionFactory = connectionFactory;
            return connectionFactory;
        });
    }

    private ConnectionFactory buildConnectionFactory(RabbitMQOptions rabbitMQOptions) {
        int port = rabbitMQOptions.getPort();
        String vhost = rabbitMQOptions.getVirtualHost();

        LOG.info(String.format("Iris configuration: host=%s, port=%s, username=%s, ssl=%s",
                rabbitMQOptions.getHost(), port, rabbitMQOptions.getUser(), rabbitMQOptions.isSsl()));

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();

            connectionFactory.setUsername(rabbitMQOptions.getUser());
            connectionFactory.setPassword((rabbitMQOptions.getPassword()));
            connectionFactory.setHost(rabbitMQOptions.getHost());
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);
            if (rabbitMQOptions.isSsl()) {
                connectionFactory.useSslProtocol();
            }

            return connectionFactory;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Could not create AMQP ConnectionFactory!", e);
            throw new IrisConnectionFactoryException("Could not create AMQP ConnectionFactory", e);
        }
    }
}
