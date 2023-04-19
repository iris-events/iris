package id.global.iris.messaging.runtime.producer;

import java.io.IOException;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.rabbitmq.client.BuiltinExchangeType;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.messaging.runtime.channel.ChannelService;

@ApplicationScoped
public class ExchangeDeclarator {
    final ChannelService channelService;
    private final String channelId;

    @Inject
    public ExchangeDeclarator(@Named("producerChannelService") final ChannelService channelService) {
        this.channelService = channelService;
        this.channelId = UUID.randomUUID().toString();
    }

    public void declareExchange(final String exchange, final ExchangeType exchangeType, final boolean isFrontend)
            throws IOException {
        final var channel = channelService.getOrCreateChannelById(channelId);
        if (isFrontend) {
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, false);
        } else {
            final var type = BuiltinExchangeType.valueOf(exchangeType.name());
            channel.exchangeDeclare(exchange, type, true);
        }
    }
}
