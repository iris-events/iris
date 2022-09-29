package id.global.iris.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.configuration.IrisConfiguration;
import id.global.iris.messaging.runtime.health.IrisLivenessCheck;
import id.global.iris.messaging.runtime.health.IrisReadinessCheck;

@ApplicationScoped
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisConfiguration configuration, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, configuration, readinessCheck, livenessCheck);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }
}
