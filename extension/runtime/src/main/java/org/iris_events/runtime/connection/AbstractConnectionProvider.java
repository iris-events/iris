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
import com.rabbitmq.client.RecoverableConnection;
import com.rabbitmq.client.RecoveryListener;

public abstract class AbstractConnectionProvider implements RecoveryListener {
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

    public synchronized Connection getConnection() {
        if (isConnectionOpen() || isConnecting()) {
            return connection;
        }

        log.info("Establishing new AMQP connection with resilience.");
        setConnecting(true);
        connect();

        if (this.connection instanceof RecoverableConnection) {
            ((RecoverableConnection) this.connection).addRecoveryListener(this);
        }

        return connection;
    }

    protected abstract String getConnectionNamePrefix();

    private void connect() {
        try {
            final var connectionName = getConnectionNamePrefix() + instanceInfoProvider.getInstanceName();
            this.connection = connectionFactoryProvider.getConnectionFactory()
                    .newConnection(connectionName);
            setConnecting(false);
            setTimedOut(false);
        } catch (IOException | TimeoutException e) {
            throw new IrisConnectionException("Could not create new AMQP connection", e);
        }
    }

    protected void setConnecting(boolean connecting) {
        this.connecting.set(connecting);
        this.readinessCheck.setConnecting(connecting);
    }

    protected void setTimedOut(boolean timedOut) {
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
