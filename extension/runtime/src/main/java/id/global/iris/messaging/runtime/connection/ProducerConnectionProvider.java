package id.global.iris.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.configuration.IrisResilienceConfig;
import id.global.iris.messaging.runtime.health.IrisLivenessCheck;
import id.global.iris.messaging.runtime.health.IrisReadinessCheck;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ProducerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ProducerConnectionProvider.class);

    private static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisResilienceConfig resilienceConfig, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, resilienceConfig, readinessCheck, livenessCheck, log);
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
