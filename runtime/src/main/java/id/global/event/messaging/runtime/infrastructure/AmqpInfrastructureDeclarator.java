package id.global.event.messaging.runtime.infrastructure;

import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_MESSAGE_TTL;
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import id.global.common.iris.Exchanges;
import id.global.common.iris.Queues;
import id.global.event.messaging.runtime.channel.ProducerChannelService;

@ApplicationScoped
public class AmqpInfrastructureDeclarator {

    private static final Logger log = LoggerFactory.getLogger(AmqpInfrastructureDeclarator.class);
    public static final int RETRY_QUEUE_TTL = 5000;
    public static final String CHANNEL_ID = "infrastructureDeclarator";

    private Channel channel;

    @Inject
    ProducerChannelService channelService;

    public void declareBackboneInfrastructure() throws IOException {
        log.info("Initializing backbone Iris infrastructure (exchanges, queues).");
        channel = channelService.getOrCreateChannelById(CHANNEL_ID);
        declareError();
        declareRetry();
        declareDeadLetter();
        declareFrontend();
        declareClient();
    }

    private void declareError() throws IOException {
        channel.exchangeDeclare(Exchanges.ERROR, BuiltinExchangeType.TOPIC, true);
        try {
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(Queues.ERROR, false, false, false, emptyMap());
            log.info("queue: {}, consumers: {}, message count: {}", declareOk.getQueue(), declareOk.getConsumerCount(),
                    declareOk.getMessageCount());
        } catch (IOException e) {
            long msgCount = channel.messageCount(Queues.ERROR);
            if (msgCount <= 0) {
                channel.queueDelete(Queues.ERROR, false, true);
                channel.queueDeclare(Queues.ERROR, false, false, false, emptyMap());
            } else {
                log.error("The new settings of queue was not set, because was not empty! queue={}", Queues.ERROR, e);
            }
        }
        channel.queueBind(Queues.ERROR, Exchanges.ERROR, "*");
    }

    private void declareRetry() throws IOException {
        final var args = Map.<String, Object> of(X_MESSAGE_TTL, RETRY_QUEUE_TTL);
        channel.exchangeDeclare(Exchanges.RETRY, BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(Queues.RETRY, true, false, false, args);
        channel.queueBind(Queues.RETRY, Exchanges.RETRY, Queues.RETRY);
    }

    private void declareDeadLetter() throws IOException {
        channel.exchangeDeclare(Exchanges.DEAD_LETTER, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(Queues.DEAD_LETTER, true, false, false, null);
        channel.queueBind(Queues.DEAD_LETTER, Exchanges.DEAD_LETTER, "#");
    }

    private void declareFrontend() throws IOException {
        channel.exchangeDeclare(Exchanges.FRONTEND, BuiltinExchangeType.TOPIC, false);
    }

    private void declareClient() throws IOException {
        channel.exchangeDeclare(Exchanges.SESSION, BuiltinExchangeType.TOPIC, true, false, null);
        channel.exchangeDeclare(Exchanges.USER, BuiltinExchangeType.TOPIC, true, false, null);
        channel.exchangeDeclare(Exchanges.BROADCAST, BuiltinExchangeType.TOPIC, true, false, null);
    }
}
