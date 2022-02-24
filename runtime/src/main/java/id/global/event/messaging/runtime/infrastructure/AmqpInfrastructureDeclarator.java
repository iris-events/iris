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

    private static final String BROADCAST_EXCHANGE = Exchanges.BROADCAST.getValue();
    private static final String USER_EXCHANGE = Exchanges.USER.getValue();
    private static final String SESSION_EXCHANGE = Exchanges.SESSION.getValue();
    private static final String FRONTEND_EXCHANGE = Exchanges.FRONTEND.getValue();
    private static final String DEAD_LETTER_EXCHANGE = Exchanges.DEAD_LETTER.getValue();
    private static final String RETRY_EXCHANGE = Exchanges.RETRY.getValue();
    private static final String ERROR_EXCHANGE = Exchanges.ERROR.getValue();
    private static final String ERROR_QUEUE = Queues.ERROR.getValue();
    private static final String RETRY_QUEUE = Queues.RETRY.getValue();
    private static final String DEAD_LETTER_QUEUE = Queues.DEAD_LETTER.getValue();

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
        channel.exchangeDeclare(ERROR_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        try {
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(ERROR_QUEUE, false, false, false, emptyMap());
            log.info("queue: {}, consumers: {}, message count: {}", declareOk.getQueue(), declareOk.getConsumerCount(),
                    declareOk.getMessageCount());
        } catch (IOException e) {
            long msgCount = channel.messageCount(ERROR_QUEUE);
            if (msgCount <= 0) {
                channel.queueDelete(ERROR_QUEUE, false, true);
                channel.queueDeclare(ERROR_QUEUE, false, false, false, emptyMap());
            } else {
                log.error("The new settings of queue was not set, because was not empty! queue={}", ERROR_QUEUE, e);
            }
        }
        channel.queueBind(ERROR_QUEUE, ERROR_EXCHANGE, "*");
    }

    private void declareRetry() throws IOException {
        final var args = Map.<String, Object> of(X_MESSAGE_TTL, RETRY_QUEUE_TTL);
        channel.exchangeDeclare(RETRY_EXCHANGE, BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(RETRY_QUEUE, true, false, false, args);
        channel.queueBind(RETRY_QUEUE, RETRY_EXCHANGE, RETRY_QUEUE);
    }

    private void declareDeadLetter() throws IOException {
        channel.exchangeDeclare(DEAD_LETTER_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(DEAD_LETTER_QUEUE, true, false, false, null);
        channel.queueBind(DEAD_LETTER_QUEUE, DEAD_LETTER_EXCHANGE, "#");
    }

    private void declareFrontend() throws IOException {
        channel.exchangeDeclare(FRONTEND_EXCHANGE, BuiltinExchangeType.TOPIC, false);
    }

    private void declareClient() throws IOException {
        channel.exchangeDeclare(SESSION_EXCHANGE, BuiltinExchangeType.TOPIC, true, false, null);
        channel.exchangeDeclare(USER_EXCHANGE, BuiltinExchangeType.TOPIC, true, false, null);
        channel.exchangeDeclare(BROADCAST_EXCHANGE, BuiltinExchangeType.TOPIC, true, false, null);
    }
}
