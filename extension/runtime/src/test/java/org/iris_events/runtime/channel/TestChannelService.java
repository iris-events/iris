package org.iris_events.runtime.channel;

import jakarta.inject.Inject;

import org.iris_events.runtime.configuration.IrisConfig;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;

public class TestChannelService extends AbstractChannelService {

    @Inject
    public TestChannelService(ConsumerConnectionProvider connectionProvider, IrisConfig config) {
        super(connectionProvider, config);
    }
}
