package id.global.event.messaging.runtime.channel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.connection.ConsumerConnectionProvider;

@ApplicationScoped
public class ConsumerChannelService extends AbstractChannelService {

    @Inject
    public ConsumerChannelService(ConsumerConnectionProvider connectionProvider, AmqpConfiguration configuration) {
        super(connectionProvider, configuration);
    }
}
