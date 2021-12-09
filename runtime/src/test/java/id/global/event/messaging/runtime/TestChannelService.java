package id.global.event.messaging.runtime;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import id.global.event.messaging.runtime.channel.ChannelService;

public class TestChannelService implements ChannelService {
    @Override
    public Channel getOrCreateChannelById(String channelId) throws IOException {
        return new TestChannel();
    }

    @Override
    public void removeChannel(String oldChannelId) throws IOException {

    }
}
