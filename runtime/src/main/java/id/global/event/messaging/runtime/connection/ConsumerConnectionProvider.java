package id.global.event.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            HostnameProvider hostnameProvider, AmqpConfiguration configuration) {
        super(connectionFactoryProvider, hostnameProvider, configuration);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }
}
