package org.iris_events.runtime.connection;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.exception.IrisConnectionFactoryException;
import org.iris_events.runtime.configuration.IrisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.RecoveryDelayHandler;

@ApplicationScoped
public class ConnectionFactoryProvider implements RecoveryDelayHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryProvider.class);

    private final IrisConfig config;

    private ConnectionFactory connectionFactory;

    @Inject
    public ConnectionFactoryProvider(IrisConfig config) {
        this.config = config;
    }

    public ConnectionFactory getConnectionFactory() {
        return Optional.ofNullable(connectionFactory).orElseGet(() -> {
            final var connectionFactory = buildConnectionFactory(config);
            this.connectionFactory = connectionFactory;
            return connectionFactory;
        });
    }

    private ConnectionFactory buildConnectionFactory(IrisConfig config) {
        int port = config.getPort();
        String vhost = config.getVirtualHost();

        log.info(String.format("Iris AMQP connection config: host=%s, port=%s, username=%s, ssl=%s",
                config.getHost(), port, config.getUsername(), config.isSsl()));

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUsername(config.getUsername());
            connectionFactory.setPassword((config.getPassword()));
            connectionFactory.setHost(config.getHost());
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);

            connectionFactory.setAutomaticRecoveryEnabled(true);
            connectionFactory.setRecoveryDelayHandler(this);

            if (config.isSsl()) {
                connectionFactory.useSslProtocol(SSLContext.getDefault());
                connectionFactory.enableHostnameVerification();
            }

            return connectionFactory;
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not create AMQP ConnectionFactory!", e);
            throw new IrisConnectionFactoryException("Could not create AMQP ConnectionFactory", e);
        }
    }

    @Override
    public long getDelay(final int recoveryAttempts) {
        final var backoffIntervalMillis = config.getBackoffIntervalMillis();
        final var backoffMultiplier = config.getBackoffMultiplier();

        return (long) Math.min((backoffIntervalMillis * backoffMultiplier * recoveryAttempts), 30000);
    }
}
