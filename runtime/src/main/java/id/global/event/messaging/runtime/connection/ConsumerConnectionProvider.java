package id.global.event.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, AmqpConfiguration configuration) {
        super(connectionFactoryProvider, instanceInfoProvider, configuration);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }
}
