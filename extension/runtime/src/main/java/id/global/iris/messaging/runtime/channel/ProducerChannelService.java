package id.global.iris.messaging.runtime.channel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import id.global.iris.messaging.runtime.configuration.IrisResilienceConfig;
import id.global.iris.messaging.runtime.connection.ProducerConnectionProvider;

@Named("producerChannelService")
@ApplicationScoped
public class ProducerChannelService extends AbstractChannelService {
    @Inject
    public ProducerChannelService(ProducerConnectionProvider connectionProvider, IrisResilienceConfig resilienceConfig) {
        super(connectionProvider, resilienceConfig);
    }
}
