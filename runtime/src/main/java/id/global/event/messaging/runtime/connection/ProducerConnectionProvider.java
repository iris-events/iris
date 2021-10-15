package id.global.event.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class ProducerConnectionProvider extends AbstractConnectionProvider {

    public static final String PRODUCER_PREFIX = "producer_";

    @Inject
    public ProducerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            HostnameProvider hostnameProvider, AmqpConfiguration configuration) {
        super(connectionFactoryProvider, hostnameProvider, configuration);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return PRODUCER_PREFIX;
    }
}
