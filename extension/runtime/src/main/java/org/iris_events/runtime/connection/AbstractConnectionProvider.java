package org.iris_events.runtime.connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.iris_events.exception.IrisConnectionException;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.configuration.IrisConfig;
import org.slf4j.Logger;

import com.rabbitmq.client.Connection;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

public abstract class AbstractConnectionProvider {
    private ConnectionFactoryProvider connectionFactoryProvider;
    private InstanceInfoProvider instanceInfoProvider;
    private IrisConfig config;
    private IrisReadinessCheck readinessCheck;
    private IrisLivenessCheck livenessCheck;
    private Logger log;
    private AtomicBoolean connecting;
    protected Connection connection;

    @SuppressWarnings("unused")
    public AbstractConnectionProvider() {
    }

    public AbstractConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisConfig config, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck, Logger log) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.instanceInfoProvider = instanceInfoProvider;
        this.config = config;
        this.readinessCheck = readinessCheck;
        this.livenessCheck = livenessCheck;
        this.log = log;
        this.connecting = new AtomicBoolean(false);
    }

    public Connection getConnection() {
        if (isConnectionOpen() || isConnecting()) {
            return connection;
        }

        log.info("Establishing new AMQP connection with resilience.");
        setConnecting(true);
        this.connection = connectWithResilience(
                config.getBackoffIntervalMillis(),
                config.getBackoffMultiplier(),
                config.getMaxRetries());

        return connection;
    }

    protected abstract String getConnectionNamePrefix();

    private Connection connectWithResilience(final long initialInterval, final double multiplier, final int maxRetries) {
        final var intervalFn = IntervalFunction.ofExponentialBackoff(initialInterval, multiplier);

        final var retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(intervalFn)
                .retryExceptions(IrisConnectionException.class)
                .failAfterMaxAttempts(true)
                .build();
        final var retry = Retry.of("executeConnection", retryConfig);
        registerEventPublisher(retry.getEventPublisher());

        final var connectFn = Retry.decorateFunction(retry, v -> connect());

        return connectFn.apply(null);
    }

    private void registerEventPublisher(Retry.EventPublisher eventPublisher) {
        eventPublisher.onRetry(onRetryEvent -> {
            log.warn(String.format("Establishing AMQP connection - retry. attempt: %d/%d, interval: %ds, last exception: %s",
                    onRetryEvent.getNumberOfRetryAttempts(),
                    config.getMaxRetries(),
                    onRetryEvent.getWaitInterval().getSeconds(),
                    onRetryEvent.getLastThrowable()));
            setConnecting(true);
            setTimedOut(false);
        });

        eventPublisher.onError(onErrorEvent -> {
            log.error(String.format("Error establishing AMQP connection. attempt: %d/%d",
                    onErrorEvent.getNumberOfRetryAttempts(),
                    config.getMaxRetries()));
            setConnecting(false);
            setTimedOut(true);
        });

        eventPublisher.onSuccess(onSuccessEvent -> {
            log.info(String.format("AMQP connection established. attempt: %d/%d",
                    onSuccessEvent.getNumberOfRetryAttempts(),
                    config.getMaxRetries()));
            setConnecting(false);
            setTimedOut(false);
        });

        eventPublisher.onIgnoredError(onIgnoredEvent -> {
            log.error(String.format("Ignored exception encountered while establishing AMQP connection."
                    + " attempt: %d/%d, last exception: %s",
                    onIgnoredEvent.getNumberOfRetryAttempts(),
                    config.getMaxRetries(),
                    onIgnoredEvent.getLastThrowable()));
        });
    }

    private Connection connect() {
        try {
            Connection connection = connectionFactoryProvider.getConnectionFactory()
                    .newConnection(getConnectionNamePrefix() + instanceInfoProvider.getInstanceName());
            setConnecting(false);
            setTimedOut(false);
            return connection;
        } catch (IOException | TimeoutException e) {
            throw new IrisConnectionException("Could not create new AMQP connection", e);
        }
    }

    private void setConnecting(boolean connecting) {
        this.connecting.set(connecting);
        this.readinessCheck.setConnecting(connecting);
    }

    private void setTimedOut(boolean timedOut) {
        this.readinessCheck.setTimedOut(timedOut);
        this.livenessCheck.setTimedOut(timedOut);
    }

    private boolean isConnecting() {
        return this.connecting.get();
    }

    private boolean isConnectionOpen() {
        return connection != null && connection.isOpen();
    }
}
