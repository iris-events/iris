package org.iris_events.runtime.channel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.runtime.connection.ProducerConnectionProvider;

@Named("producerChannelService")
@ApplicationScoped
public class ProducerChannelService extends AbstractChannelService {
    @Inject
    public ProducerChannelService(ProducerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
