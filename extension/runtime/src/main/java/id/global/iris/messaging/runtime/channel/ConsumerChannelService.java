package id.global.iris.messaging.runtime.channel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.connection.ConsumerConnectionProvider;

@Named("consumerChannelService")
@ApplicationScoped()
public class ConsumerChannelService extends AbstractChannelService {
    @Inject
    public ConsumerChannelService(ConsumerConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        super(connectionProvider, config);
    }
}
