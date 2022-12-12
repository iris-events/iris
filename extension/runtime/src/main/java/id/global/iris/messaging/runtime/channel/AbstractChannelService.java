package id.global.iris.messaging.runtime.channel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.connection.AbstractConnectionProvider;
import id.global.iris.messaging.runtime.exception.IrisConnectionException;

public abstract class AbstractChannelService implements ChannelService, ShutdownListener {
    private final static Logger log = LoggerFactory.getLogger(AbstractChannelService.class);
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    private AbstractConnectionProvider connectionProvider;
    private IrisRabbitMQConfig config;

    @SuppressWarnings("unused")
    protected AbstractChannelService() {
    }

    protected AbstractChannelService(AbstractConnectionProvider connectionProvider, IrisRabbitMQConfig config) {
        this.connectionProvider = connectionProvider;
        this.config = config;
    }

    @Override
    public Channel getOrCreateChannelById(String channelId) {
        Channel channel = channelMap.computeIfAbsent(channelId, t -> createChannel());
        if (channel == null) {
            throw new IrisConnectionException("Could not create channel.");
        }
        return channel;
    }

    @Override
    public void removeChannel(String oldChannelId) {
        Channel channel = channelMap.get(oldChannelId);
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.warn(String.format("Exception while closing channel %s", oldChannelId), e);
            }
        }
        channelMap.remove(oldChannelId);
    }

    @Override
    public Channel createChannel() throws RuntimeException {
        try {
            Channel channel = connectionProvider.getConnection().createChannel();

            if (channel != null && config.getConfirmationBatchSize() > 0) {
                channel.confirmSelect();
            }
            return channel;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void removeAllChannels() {
        channelMap.clear();
    }

    @Override public void shutdownCompleted(final ShutdownSignalException cause) {
        this.removeAllChannels();
    }
}
