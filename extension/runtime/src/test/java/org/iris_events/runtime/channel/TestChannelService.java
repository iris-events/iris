package org.iris_events.runtime.channel;

import jakarta.inject.Inject;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;

public class TestChannelService extends AbstractChannelService {

    @Inject
    public TestChannelService(ConsumerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
