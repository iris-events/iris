package id.global.event.messaging.runtime.channel;

import java.io.IOException;

import com.rabbitmq.client.Channel;

public interface ChannelService {
    Channel getOrCreateChannelById(String channelId) throws IOException;

    void removeChannel(String oldChannelId) throws IOException;
}
