package org.iris_events.runtime.connection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;

@ApplicationScoped
public class ConsumerConnectionProvider extends AbstractConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(ConsumerConnectionProvider.class);
    private static final String CONSUMER_PREFIX = "consumer_";

    @Inject
    public ConsumerConnectionProvider(ConnectionFactoryProvider connectionFactoryProvider,
                                      InstanceInfoProvider instanceInfoProvider, IrisRabbitMQConfig config, IrisReadinessCheck readinessCheck,
                                      IrisLivenessCheck livenessCheck) {
        super(connectionFactoryProvider, instanceInfoProvider, config, readinessCheck, livenessCheck, log);
    }

    @Override
    protected String getConnectionNamePrefix() {
        return CONSUMER_PREFIX;
    }
}
