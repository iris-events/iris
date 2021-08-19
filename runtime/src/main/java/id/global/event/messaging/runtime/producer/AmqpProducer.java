package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = Logger.getLogger(AmqpProducer.class.getName());
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Connection connection;
    private final ExecutorService pool;
    private final String hostName;
    private Optional<Channel> channel = Optional.empty();
    private final AtomicInteger failCounter = new AtomicInteger(0);
    private final Object lock = new Object();
    private final AtomicInteger count = new AtomicInteger(0);
    private boolean connected;

    private final ThreadLocal<Channel> channels = new ThreadLocal<Channel>() {
        private static final Logger LOG = Logger.getLogger(ThreadLocal.class);

        @Override
        public Channel get() {
            return super.get();
        }
    };

    @Inject
    public AmqpProducer(AmqpConfiguration configuration, ObjectMapper objectMapper) {
        pool = Executors.newFixedThreadPool(6); //TODO: set pool according to needs (should this be in configuration?)
        this.amqpConfiguration = configuration;
        this.objectMapper = objectMapper;
        this.hostName = Common.getHostName();
        connect();
    }

    public AmqpConfiguration getAmqpConfiguration() {
        return amqpConfiguration;
    }

    private void connect() {

        if (this.connection != null)
            return;

        failCounter.set(0);

        ConnectionFactory factory = Common.getConnectionFactory(amqpConfiguration);

        while (!connected && failCounter.get() < 10) {
            try {
                LOG.info("Connecting...");
                connection = factory.newConnection("producer_" + hostName);
                if (this.connection.isOpen()) {
                    connected = true;
                    LOG.info("Connected!");

                }
            } catch (IOException | TimeoutException e) {
                LOG.error("Connection failed!", e);
                connected = false;
            }
            if (!this.connected) {
                try {
                    LOG.warn("Waiting to retry connection! #" + failCounter.incrementAndGet());
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }

    }

    /**
     *
     * @param exchange exchange name to which we send message to
     * @param routingKey routing key to route message to queue
     * @param type exchange type (DIRECT, FANOUT, TOPIC)
     * @param message message to be send
     * @param properties additinal properties for
     * @param failImmediately fail immediately on publishing error
     * @return true/false if message was published successfult to broker
     */
    public boolean publish(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            AMQP.BasicProperties properties, boolean failImmediately) {
        switch (type) {
            case TOPIC -> {
                if (routingKey.isPresent()) {
                    return publishTopic(exchange, routingKey.get(), message, properties, failImmediately);
                }
            }
            case DIRECT -> {
                return publishDirect(exchange, routingKey, message, properties, failImmediately);
            }
            case FANOUT -> {
                return publishFanout(exchange, message, properties, failImmediately);
            }
            default -> {
                LOG.warn("Exchange type unknown!");
                return false;
            }
        }
        return false;
    }

    private boolean publishDirect(String exchange, Optional<String> routingKey, Object message,
            AMQP.BasicProperties properties, boolean immediate) {

        try {
            routingKey = routingKey.filter(s -> !s.isEmpty());
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(exchange,
                    routingKey.orElse(message.getClass().getSimpleName().toLowerCase()),
                    properties,
                    bytes, immediate);
        } catch (Exception e) {
            LOG.error("Sending to exchange: [" + exchange + "] with routingKey: [" + routingKey + "] failed! ", e);
            return false;
        }
    }

    private boolean publishTopic(String exchange, String topicRoutingKey, Object message, AMQP.BasicProperties properties,
            boolean immediate) {
        //TODO: maybe validate topic (routing key)?

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(exchange,
                    topicRoutingKey,
                    properties,
                    bytes,
                    immediate);
        } catch (Exception e) {
            LOG.error("Sending to topic exchange: [" + exchange + "] with routingKey: [" + topicRoutingKey + "] failed! ", e);
            return false;
        }
    }

    private boolean publishFanout(String fanoutExchange, Object message, AMQP.BasicProperties properties, boolean immediate) {
        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(fanoutExchange,
                    "",
                    properties,
                    bytes, immediate);
        } catch (Exception e) {
            LOG.error("Sending to fanout exchange: [" + fanoutExchange + "] failed! ", e);
            return false;
        }
    }

    /**
     * @param exchange exchange nam that message will be send to
     * @param routingKey optinal routingKey, if not provided, className of message send will be used
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishDirectAsync(String exchange, Optional<String> routingKey, Object message,
            AMQP.BasicProperties properties)
            throws Exception {

        routingKey = routingKey.filter(s -> !s.isEmpty());
        final byte[] bytes = objectMapper.writeValueAsBytes(message);
        publishMessageAsync(exchange,
                routingKey.orElse(message.getClass().getSimpleName().toLowerCase()),
                properties,
                bytes);
    }

    /**
     * @param fanoutExchange exchange name where message will be send
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishFanoutAsync(String fanoutExchange, Object message, AMQP.BasicProperties properties)
            throws Exception {
        final byte[] bytes = objectMapper.writeValueAsBytes(message);
        publishMessageAsync(fanoutExchange, "", properties, bytes);
    }

    /**
     * @param exchange exchange name where message will be send
     * @param topicRoutingKey topicRoutingKey for message routing example: log.internal.warn, log.external.warn,
     *        log.internal.error
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishTopicAsync(String exchange, String topicRoutingKey, Object message, AMQP.BasicProperties properties)
            throws Exception {
        final byte[] bytes = objectMapper.writeValueAsBytes(message);
        publishMessageAsync(exchange, topicRoutingKey, properties, bytes);
    }

    private void publishMessageAsync(final String exchange,
            final String routingKey,
            final AMQP.BasicProperties properties,
            final byte[] bytes) {

        if (this.connection.isOpen()) {
            CompletableFuture.runAsync(() -> {
                try {
                    final Channel existing = channels.get();
                    if (existing != null && existing.isOpen()) {
                        publish(exchange, routingKey, properties, bytes, Optional.of(existing));
                        existing.waitForConfirms(500);
                    } else {
                        final Channel newChannel = connection.createChannel();
                        newChannel.confirmSelect();
                        publish(exchange, routingKey, properties, bytes, Optional.of(newChannel));
                        newChannel.waitForConfirms(500);
                        channels.set(newChannel);
                    }
                } catch (Exception e) {
                    LOG.error("Message publishing failed exchange:[" + exchange + "], routingKey: [" + routingKey + "]!",
                            e);
                }
            }, pool);
        } else {
            LOG.error("Connection is not open!");
            connect();
        }

    }

    private void publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] bytes,
            Optional<Channel> channel) throws Exception {
        channel.get().basicPublish(exchange, routingKey, properties, bytes);
    }

    private Optional<Channel> createChannel() {
        try {
            return Optional.ofNullable(connection.createChannel());
        } catch (IOException e) {
            LOG.error("Failed to create channel!", e);
        }
        return Optional.empty();
    }

    private void createChannel(boolean withConfirmations) throws IOException {
        this.channel = createChannel();
        if (withConfirmations && this.channel.isPresent()) {
            this.channel.get().confirmSelect();
            this.channel.get().addConfirmListener(confirmListener);
            this.channel.get().addReturnListener(returnListener);
        }
    }

    private boolean publishMessage(final String exchange,
            final String routingKey,
            final AMQP.BasicProperties properties,
            final byte[] bytes, boolean immediate) throws Exception {
        synchronized (this.lock) {
            if (this.connection.isOpen()) {
                if (this.channel.isEmpty() || !this.channel.get().isOpen()) {
                    this.channel = Optional.empty();
                    createChannel(true);
                }
                publish(exchange, routingKey, properties, bytes, this.channel);

                if (count.incrementAndGet() == 100 || immediate) { //for every 100 messages that are send wait for all confirmations
                    this.channel.get().waitForConfirms(500); //timeout is 500ms (should we change this to longer?
                    count.set(0);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private ConfirmListener confirmListener = new ConfirmListener() {
        @Override
        public void handleAck(long deliveryTag, boolean multiple) throws IOException {
            LOG.info("Message with deliveryTag [" + deliveryTag + "] ACK!" + " Multiple: " + multiple);
        }

        @Override
        public void handleNack(long deliveryTag, boolean multiple) throws IOException {
            LOG.warn("Message with deliveryTag [" + deliveryTag + "] NACK!" + " Multiple: " + multiple);
        }
    };

    //this will be used if message has "mandatory" flag set
    private ReturnListener returnListener = new ReturnListener() {
        @Override
        public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
                AMQP.BasicProperties properties, byte[] body) throws IOException {
            LOG.error("Message returned! exchange=[" + exchange + "], routingKey=[" + routingKey + "]: replyCode=[" + replyCode
                    + "] replyMessage=[" + replyText + "]");
        }
    };

}
