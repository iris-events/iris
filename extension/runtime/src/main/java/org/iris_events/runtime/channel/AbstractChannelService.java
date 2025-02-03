package org.iris_events.runtime.channel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.iris_events.runtime.configuration.IrisConfig;
import org.iris_events.runtime.connection.AbstractConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;

public abstract class AbstractChannelService implements ChannelService {
    private final static Logger log = LoggerFactory.getLogger(AbstractChannelService.class);
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    private AbstractConnectionProvider connectionProvider;
    private IrisConfig config;

    @SuppressWarnings("unused")
    protected AbstractChannelService() {
    }

    protected AbstractChannelService(AbstractConnectionProvider connectionProvider, IrisConfig config) {
        this.connectionProvider = connectionProvider;
        this.config = config;
    }

    @Override
    public Channel getOrCreateChannelById(String channelId) {
        final var channel = channelMap.get(channelId);
        if (channel != null && channel.isOpen()) {
            return channel;
        }

        return channelMap.compute(channelId, (key, value) -> createChannel());
    }

    @Override
    public void removeChannel(String channelId) {
        Optional.ofNullable(channelMap.remove(channelId)).ifPresent(channel -> {
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException | TimeoutException e) {
                    log.warn(String.format("Exception while closing channel %s", channelId), e);
                }
            }
        });
    }

    @Override
    public Channel createChannel() throws RuntimeException {
        try {
            Channel channel = connectionProvider.getConnection().createChannel();

            if (channel != null && config.confirmationBatchSize() > 0) {
                channel.confirmSelect();
            }
            return channel;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void closeAndRemoveAllChannels() {
        channelMap.forEach((key, channel) -> {
            removeChannel(key);
        });
    }

    public ConcurrentHashMap<String, Channel> getChannelMap() {
        return channelMap;
    }
}
