package id.global.iris.messaging.runtime.channel;

import java.io.IOException;

import com.rabbitmq.client.Channel;

public interface ChannelService {
    Channel getOrCreateChannelById(String channelId) throws IOException;

    Channel createChannel() throws IOException;

    void removeChannel(String oldChannelId) throws IOException;
}
