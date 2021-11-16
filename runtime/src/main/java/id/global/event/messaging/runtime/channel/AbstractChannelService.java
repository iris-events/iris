package id.global.event.messaging.runtime.channel;

import java.io.IOException;
import java.io.UncheckedIOException;
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
        Channel channel = channelMap.computeIfAbsent(channelId, this::createChanel);
        if (channel == null) {
            throw new AmqpConnectionException("Could not create channel.");
        }
        return channel;
    }

    private Channel createChanel(String channelId) throws RuntimeException {
        try {
            Channel channel = connectionProvider.getConnection().createChannel();

            if (channel != null && configuration.getConfirmationBatchSize() > 0) {
                channel.confirmSelect();
            }
            return channel;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
