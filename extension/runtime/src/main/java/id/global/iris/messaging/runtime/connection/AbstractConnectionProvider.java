package id.global.iris.messaging.runtime.connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;

import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.configuration.IrisConfiguration;
import id.global.iris.messaging.runtime.exception.IrisConnectionException;
import id.global.iris.messaging.runtime.health.IrisLivenessCheck;
import id.global.iris.messaging.runtime.health.IrisReadinessCheck;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

public abstract class AbstractConnectionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnectionProvider.class);

    private ConnectionFactoryProvider connectionFactoryProvider;
    private InstanceInfoProvider instanceInfoProvider;
    private IrisConfiguration configuration;
    private IrisReadinessCheck readinessCheck;
    private IrisLivenessCheck livenessCheck;
    private AtomicBoolean connecting;
    protected Connection connection;

    @SuppressWarnings("unused")
    public AbstractConnectionProvider() {
    }

    public AbstractConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisConfiguration configuration, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.instanceInfoProvider = instanceInfoProvider;
        this.configuration = configuration;
        this.readinessCheck = readinessCheck;
        this.livenessCheck = livenessCheck;
        this.connecting = new AtomicBoolean(false);
    }

    public Connection getConnection() {
        if (connectionIsNullOrClosed() && !this.getConnecting()) {
            LOG.info("Starting AMQP connection with resilience");
            setConnecting(true);
            this.connection = connectWithResilience(
                    configuration.getBackoffIntervalMillis(),
                    configuration.getBackoffMultiplier(),
                    configuration.getMaxRetries());
        }
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

        LOG.info("Creating new AMQP connection.");
        return connectFn.apply(null);
    }

    private void registerEventPublisher(Retry.EventPublisher eventPublisher) {
        eventPublisher.onRetry(onRetryEvent -> {
            LOG.warn(String.format("onRetryEvent: retryAttempts: %d/%d, interval: %d, creation time: %d, last throwable: %s",
                    onRetryEvent.getNumberOfRetryAttempts(),
                    configuration.getMaxRetries(),
                    onRetryEvent.getWaitInterval().getSeconds(),
                    onRetryEvent.getCreationTime().toInstant().getEpochSecond(),
                    onRetryEvent.getLastThrowable()));
            setConnecting(true);
            setTimedOut(false);
        });

        eventPublisher.onError(onErrorEvent -> {
            LOG.error(String.format("onErrorEvent: retryAttempts: %d/%d, creation time: %d",
                    onErrorEvent.getNumberOfRetryAttempts(),
                    configuration.getMaxRetries(),
                    onErrorEvent.getCreationTime().toInstant().getEpochSecond()));
            setConnecting(false);
            setTimedOut(true);
        });

        eventPublisher.onSuccess(onSuccessEvent -> {
            LOG.info(String.format("onSuccessEvent, retryAttempts: %d/%d, creation time: %d",
                    onSuccessEvent.getNumberOfRetryAttempts(),
                    configuration.getMaxRetries(),
                    onSuccessEvent.getCreationTime().toInstant().getEpochSecond()));
            setConnecting(false);
            setTimedOut(false);
        });

        eventPublisher.onIgnoredError(onIgnoredEvent -> {
            LOG.error(String.format("onIgnoredError: retryAttempts: %d/%d, creation time: %d",
                    onIgnoredEvent.getNumberOfRetryAttempts(),
                    configuration.getMaxRetries(),
                    onIgnoredEvent.getCreationTime().toInstant().getEpochSecond()));
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

    private boolean getConnecting() {
        return this.connecting.get();
    }

    private boolean connectionIsNullOrClosed() {
        return connection == null || !connection.isOpen();
    }
}
