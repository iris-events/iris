package id.global.iris.messaging.runtime.channel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import id.global.iris.messaging.runtime.configuration.AmqpConfiguration;
import id.global.iris.messaging.runtime.connection.ProducerConnectionProvider;

@Named("producerChannelService")
@ApplicationScoped
public class ProducerChannelService extends AbstractChannelService {
    @Inject
    public ProducerChannelService(ProducerConnectionProvider connectionProvider, AmqpConfiguration configuration) {
        super(connectionProvider, configuration);
    }
}
