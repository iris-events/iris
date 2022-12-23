package id.global.iris.messaging.runtime.consumer;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;

import id.global.iris.messaging.runtime.channel.ChannelService;

@ApplicationScoped
public class QueueDeclarator {

    private static final Logger log = LoggerFactory.getLogger(QueueDeclarator.class);

    ChannelService channelService;
    private final String channelId;

    @Inject
    public QueueDeclarator(@Named("consumerChannelService") final ChannelService channelService) {
        this.channelService = channelService;
        this.channelId = UUID.randomUUID().toString();
    }

    public void declareQueueWithRecreateOnConflict(final Channel channel, final QueueDeclarationDetails details)
            throws IOException {
        final var queueName = details.queueName;
        try {
            declareQueue(details);
        } catch (IOException e) {
            long msgCount = channel.messageCount(queueName);
            if (msgCount <= 0) {
                log.warn("Queue declaration parameters changed. Trying to re-declare queue. Details: "
                        + e.getCause().getMessage());
                channel.queueDelete(queueName, false, true);
                declareQueue(details);
            } else {
                log.error("The new settings of queue was not set, because was not empty! queue={}", queueName, e);
            }
        }
    }

    private void declareQueue(QueueDeclarationDetails details) throws IOException {
        final var queueName = details.queueName;
        final var durable = details.durable;
        final var exclusive = details.exclusive;
        final var autoDelete = details.autoDelete;
        final var arguments = details.arguments;

        Channel channel = channelService.getOrCreateChannelById(this.channelId);
        final var declareOk = channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments);
        log.info("Queue declared. name: {}, durable: {}, autoDelete: {}, consumers: {}, message count: {}",
                declareOk.getQueue(),
                durable, autoDelete,
                declareOk.getConsumerCount(),
                declareOk.getMessageCount());
    }

    public record QueueDeclarationDetails(String queueName, boolean durable, boolean exclusive, boolean autoDelete,
                                          Map<String, Object> arguments) {

    }
}
