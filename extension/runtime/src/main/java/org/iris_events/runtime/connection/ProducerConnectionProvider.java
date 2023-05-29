package org.iris_events.runtime.connection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ProducerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ProducerConnectionProvider.class);

    private static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
                                      InstanceInfoProvider instanceInfoProvider, IrisRabbitMQConfig config, IrisReadinessCheck readinessCheck,
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
}
