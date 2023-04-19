package id.global.iris.messaging.runtime.channel;

import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.connection.ConsumerConnectionProvider;

import jakarta.inject.Inject;

public class TestChannelService extends AbstractChannelService {

    @Inject
    public TestChannelService(ConsumerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
