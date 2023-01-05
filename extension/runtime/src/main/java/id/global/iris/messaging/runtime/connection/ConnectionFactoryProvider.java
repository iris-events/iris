package id.global.iris.messaging.runtime.connection;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.exception.IrisConnectionFactoryException;

@ApplicationScoped
public class ConnectionFactoryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

    private final IrisRabbitMQConfig config;

    private ConnectionFactory connectionFactory;

    @Inject
    public ConnectionFactoryProvider(IrisRabbitMQConfig config) {
        this.config = config;
    }

    public ConnectionFactory getConnectionFactory() {
        return Optional.ofNullable(connectionFactory).orElseGet(() -> {
            final var connectionFactory = buildConnectionFactory(config);
            this.connectionFactory = connectionFactory;
            return connectionFactory;
        });
    }

    private ConnectionFactory buildConnectionFactory(IrisRabbitMQConfig config) {
        int port = config.getPort();
        String vhost = config.getVirtualHost();

        LOG.info(String.format("Iris AMQP connection config: host=%s, port=%s, username=%s, ssl=%s",
                config.getHost(), port, config.getUsername(), config.isSsl()));

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUsername(config.getUsername());
            connectionFactory.setPassword((config.getPassword()));
            connectionFactory.setHost(config.getHost());
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);
            if (config.isSsl()) {
                connectionFactory.useSslProtocol(SSLContext.getDefault());
                connectionFactory.enableHostnameVerification();
            }

            return connectionFactory;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Could not create AMQP ConnectionFactory!", e);
            throw new IrisConnectionFactoryException("Could not create AMQP ConnectionFactory", e);
        }
    }
}
