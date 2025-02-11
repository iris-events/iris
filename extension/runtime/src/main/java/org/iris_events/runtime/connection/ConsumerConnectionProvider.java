package org.iris_events.runtime.connection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.configuration.IrisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Recoverable;

@Singleton
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ConsumerConnectionProvider.class);
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisConfig config, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, config, readinessCheck, livenessCheck, log);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }

    @Override
    public void handleRecovery(final Recoverable recoverable) {
        log.warn("ConsumerConnectionProvider handleRecovery!");
        super.setConnecting(false);
        super.setTimedOut(false);
    }

    @Override
    public void handleRecoveryStarted(final Recoverable recoverable) {
        log.warn("ConsumerConnectionProvider handleRecoveryStarted!");
        super.setConnecting(true);
    }

    @Override
    public void handleTopologyRecoveryStarted(final Recoverable recoverable) {
        log.warn("ConsumerConnectionProvider handleTopologyRecoveryStarted!");
        super.handleTopologyRecoveryStarted(recoverable);
        super.setConnecting(true);
    }
}
