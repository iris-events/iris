package id.global.event.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class ProducerConnectionProvider extends AbstractConnectionProvider {

    private static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, AmqpConfiguration configuration) {
        super(connectionFactoryProvider, instanceInfoProvider, configuration);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return PRODUCER_PREFIX;
    }
}
