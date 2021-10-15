package id.global.event.messaging.runtime.connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;

import id.global.event.messaging.runtime.ConnectionFactoryProvider;
import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.exception.AmqpConnectionException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

public abstract class AbstractConnectionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnectionProvider.class);

    private ConnectionFactoryProvider connectionFactoryProvider;
    private HostnameProvider hostnameProvider;
    private AmqpConfiguration configuration;
    private AtomicInteger retryCount;

    protected Connection connection;

    @SuppressWarnings("unused")
    public AbstractConnectionProvider() {
    }

    public AbstractConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider, HostnameProvider hostnameProvider,
            AmqpConfiguration configuration) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.hostnameProvider = hostnameProvider;
        this.configuration = configuration;
        this.retryCount = new AtomicInteger(0);
    }

    public Connection getConnection() {
        if (connection == null || !connection.isOpen()) {
            this.connection = connectWithResilience(
                    configuration.getBackoffIntervalMillis(),
                    configuration.getBackoffMultiplier(),
                    configuration.getMaxRetries());
            retryCount.set(0);
        }
        return connection;
    }

    protected abstract String getConnectionNamePrefix();

    private Connection connectWithResilience(final long initialInterval, final double multiplier, final int maxRetries) {
        final var intervalFn = IntervalFunction.ofExponentialBackoff(initialInterval, multiplier);

        final var retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(intervalFn)
                .build();
        final var retry = Retry.of("executeConnection", retryConfig);
        final var connectFn = Retry.decorateFunction(retry, v -> connect());

        LOG.info("Creating new AMQP connection.");
        return connectFn.apply(null);
    }

    private Connection connect() {
        try {
            return connectionFactoryProvider.getConnectionFactory()
                    .newConnection(getConnectionNamePrefix() + hostnameProvider.getHostName());
        } catch (IOException | TimeoutException e) {
            String msg = String.format("Could not create new AMQP connection, retry %d/%d", retryCount.incrementAndGet(),
                    configuration.getMaxRetries());
            if (retryCount.get() >= configuration.getMaxRetries()) {
                LOG.error(msg, e);
            } else {
                LOG.warn(msg, e);
            }
            throw new AmqpConnectionException("Could not create new AMQP connection", e);
        }
    }
}
