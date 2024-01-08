package org.iris_events.runtime.channel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.runtime.configuration.IrisConfig;
import org.iris_events.runtime.connection.ProducerConnectionProvider;

@Named("producerChannelService")
@ApplicationScoped
public class ProducerChannelService extends AbstractChannelService {
    @Inject
    public ProducerChannelService(ProducerConnectionProvider connectionProvider, IrisConfig config) {
        super(connectionProvider, config);
    }
}
