package id.global.iris.messaging.runtime.connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.configuration.IrisResilienceConfig;
import id.global.iris.messaging.runtime.health.IrisLivenessCheck;
import id.global.iris.messaging.runtime.health.IrisReadinessCheck;

@ApplicationScoped
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ConsumerConnectionProvider.class);
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
            InstanceInfoProvider instanceInfoProvider, IrisResilienceConfig resilienceConfig, IrisReadinessCheck readinessCheck,
            IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, resilienceConfig, readinessCheck, livenessCheck, log);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }
}
