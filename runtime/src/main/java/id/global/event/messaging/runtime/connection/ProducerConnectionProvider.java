package id.global.event.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.health.IrisHealthCheck;

@ApplicationScoped
public class ProducerConnectionProvider extends AbstractConnectionProvider {
    private static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, AmqpConfiguration configuration, IrisHealthCheck healthCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, configuration, healthCheck);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return PRODUCER_PREFIX;
    }
}
