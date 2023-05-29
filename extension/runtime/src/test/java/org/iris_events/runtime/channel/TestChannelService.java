package org.iris_events.runtime.channel;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;

import jakarta.inject.Inject;

public class TestChannelService extends AbstractChannelService {

    @Inject
    public TestChannelService(ConsumerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
