package id.global.event.messaging.runtime.channel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.rabbitmq.client.Channel;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.connection.ProducerConnectionProvider;

@ApplicationScoped
public class ProducerChannelService {

    private final ProducerConnectionProvider connectionProvider;
    private final AmqpConfiguration configuration;
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();

    @Inject
    public ProducerChannelService(ProducerConnectionProvider connectionProvider, AmqpConfiguration configuration) {
        this.connectionProvider = connectionProvider;
        this.configuration = configuration;
    }

    public Channel getChannel(String channelKey) throws IOException {
        if (channelMap.get(channelKey) != null) {
            return channelMap.get(channelKey);
        }

        Channel channel = connectionProvider.getConnection().createChannel();
        if (configuration.getConfirmationBatchSize() > 0) {
            channel.confirmSelect();
        }
        channelMap.put(channelKey, channel);
        return channelMap.get(channelKey);
    }

}
