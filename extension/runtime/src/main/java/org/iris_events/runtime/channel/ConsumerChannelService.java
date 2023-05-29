package org.iris_events.runtime.channel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;

@Named("consumerChannelService")
@ApplicationScoped()
public class ConsumerChannelService extends AbstractChannelService {
    @Inject
    public ConsumerChannelService(ConsumerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
