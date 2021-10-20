package id.global.event.messaging.runtime.channel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.rabbitmq.client.Channel;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.connection.AbstractConnectionProvider;
import id.global.event.messaging.runtime.exception.AmqpConnectionException;

public abstract class AbstractChannelService {
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    private AbstractConnectionProvider connectionProvider;
    private AmqpConfiguration configuration;

    @SuppressWarnings("unused")
    protected AbstractChannelService() {
    }

    protected AbstractChannelService(AbstractConnectionProvider connectionProvider, AmqpConfiguration configuration) {
        this.connectionProvider = connectionProvider;
        this.configuration = configuration;
    }

    public Channel getOrCreateChannelById(String channelId) throws IOException {
        if (channelMap.get(channelId) != null) {
            return channelMap.get(channelId);
        }

        Channel channel = connectionProvider.getConnection().createChannel();
        if (channel == null) {
            throw new AmqpConnectionException("Could not create channel.");
        }

        if (configuration.getConfirmationBatchSize() > 0) {
            channel.confirmSelect();
        }
        channelMap.put(channelId, channel);
        return channelMap.get(channelId);
    }
}
