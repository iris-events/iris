package org.iris_events.runtime.connection;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.configuration.IrisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Recoverable;

import io.quarkus.runtime.StartupEvent;

@Singleton
public class ProducerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ProducerConnectionProvider.class);

    private static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisConfig config, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, config, readinessCheck, livenessCheck, log);
    }

    /**
     * When service is only producing events, it needs to obtain connection in order to appear ready for Health check.
     */
    public void onApplicationStart(@Observes StartupEvent event) {
        getConnection();
    }

    @Override
    protected String getConnectionNamePrefix() {
        return PRODUCER_PREFIX;
    }

    @Override
    public void handleRecovery(final Recoverable recoverable) {
        log.warn("ProducerConnectionProvider handleRecovery!");
        super.setConnecting(false);
        super.setTimedOut(false);
    }

    @Override
    public void handleRecoveryStarted(final Recoverable recoverable) {
        log.warn("ProducerConnectionProvider handleRecoveryStarted!");
        super.setConnecting(true);
    }

    @Override
    public void handleTopologyRecoveryStarted(final Recoverable recoverable) {
        log.warn("ProducerConnectionProvider handleTopologyRecoveryStarted!");
        super.handleTopologyRecoveryStarted(recoverable);
        super.setConnecting(true);
    }
}
