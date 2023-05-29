package org.iris_events.runtime.channel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.rabbitmq.client.Channel;

public interface ChannelService {
    Channel getOrCreateChannelById(String channelId) throws IOException;

    Channel createChannel() throws IOException;

    void removeChannel(String oldChannelId) throws IOException;

    void closeAndRemoveAllChannels();

    ConcurrentHashMap<String, Channel> getChannelMap();
}
