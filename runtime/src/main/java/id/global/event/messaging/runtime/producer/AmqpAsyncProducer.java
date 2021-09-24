package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ReturnListener;

import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.EventContext;

public class AmqpAsyncProducer {
    private static final Logger LOG = Logger.getLogger(AmqpProducer.class.getName());
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Connection connection;
    private final ExecutorService pool;
    private final String hostName;
    private final AtomicInteger failCounter = new AtomicInteger(0);
    private boolean connected;
    private final EventContext eventContext;
    private final long waitTimeout = 2000;

    private final ThreadLocal<Channel> channels = new ThreadLocal<>() {
        @Override
        public Channel get() {
            return super.get();
        }
    };

    @Inject
    public AmqpAsyncProducer(AmqpConfiguration configuration, ObjectMapper objectMapper,
            EventContext eventContext) {
        pool = Executors.newFixedThreadPool(6); //TODO: set pool according to needs (should this be in configuration?)
        this.amqpConfiguration = configuration;
        this.objectMapper = objectMapper;
        this.hostName = Common.getHostName();
        this.eventContext = eventContext;

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

    public void publishAsync(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            AMQP.BasicProperties properties) {

        routePublish(exchange, routingKey, type, message, properties);
    }

    public void publishAsync(String exchange, Optional<String> routingKey, ExchangeType type, Object message) {

        routePublish(exchange, routingKey, type, message, null);
    }

    private void routePublish(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            AMQP.BasicProperties properties) {
        if (properties == null) {
            properties = this.eventContext.getAmqpBasicProperties();
        }
        switch (type) {
            case TOPIC -> {
                if (routingKey.isPresent()) {
                    publishTopicAsync(exchange, routingKey.get(), message, properties);
                }
            }
            case DIRECT -> publishDirectAsync(exchange, routingKey, message, properties);
            case FANOUT -> publishFanoutAsync(exchange, message, properties);
            default -> LOG.error("Exchange type unknown!");
        }

    }

    /**
     * @param exchange exchange nam that message will be send to
     * @param routingKey optinal routingKey, if not provided, className of message send will be used
     * @param message Object/message to be send to exchange
     */
    public void publishDirectAsync(String exchange, Optional<String> routingKey, Object message,
            AMQP.BasicProperties properties) {
        try {
            if (properties == null) {
                properties = this.eventContext.getAmqpBasicProperties();
            }
            routingKey = routingKey.filter(s -> !s.isEmpty());
            final byte[] bytes;

            bytes = objectMapper.writeValueAsBytes(message);

            publishMessageAsync(exchange,
                    routingKey.orElse(message.getClass().getSimpleName().toLowerCase()),
                    properties,
                    bytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fanoutExchange exchange name where message will be send
     * @param message Object/message to be send to exchange
     */
    public void publishFanoutAsync(String fanoutExchange, Object message, AMQP.BasicProperties properties) {
        try {

            if (properties == null) {
                properties = this.eventContext.getAmqpBasicProperties();
            }

            final byte[] bytes = objectMapper.writeValueAsBytes(message);

            publishMessageAsync(fanoutExchange, "", properties, bytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param exchange exchange name where message will be send
     * @param topicRoutingKey topicRoutingKey for message routing example: log.internal.warn, log.external.warn,
     *        log.internal.error
     * @param message Object/message to be send to exchange
     */
    public void publishTopicAsync(String exchange, String topicRoutingKey, Object message, AMQP.BasicProperties properties) {
        try {
            if (properties == null) {
                properties = this.eventContext.getAmqpBasicProperties();
            }
            final byte[] bytes = objectMapper.writeValueAsBytes(message);

            publishMessageAsync(exchange, topicRoutingKey, properties, bytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void publishMessageAsync(final String exchange,
            final String routingKey,
            AMQP.BasicProperties properties,
            final byte[] bytes) {

        if (this.connection.isOpen()) {
            if (properties == null) {
                properties = this.eventContext.getAmqpBasicProperties();
            }
            AMQP.BasicProperties finalProperties = properties;
            CompletableFuture.runAsync(() -> {
                try {
                    final Channel existing = channels.get();
                    if (existing != null && existing.isOpen()) {
                        publish(exchange, routingKey, finalProperties, bytes, Optional.of(existing));
                        existing.waitForConfirms(waitTimeout);
                    } else {
                        final Channel newChannel = createChannel();
                        newChannel.confirmSelect();
                        publish(exchange, routingKey, finalProperties, bytes, Optional.of(newChannel));
                        newChannel.waitForConfirms(waitTimeout);
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
        // TODO handle optional
        channel.get().basicPublish(exchange, routingKey, properties, bytes);
    }

    private Channel createChannel() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            LOG.error("Failed to create channel!", e);
        }
        return null;
    }

    private final ConfirmListener confirmListener = new ConfirmListener() {
        @Override
        public void handleAck(long deliveryTag, boolean multiple) {
            LOG.info("Message with deliveryTag [" + deliveryTag + "] ACK!" + " Multiple: " + multiple);
        }

        @Override
        public void handleNack(long deliveryTag, boolean multiple) {
            LOG.warn("Message with deliveryTag [" + deliveryTag + "] NACK!" + " Multiple: " + multiple);
        }
    };

    //this will be used if message has "mandatory" flag set
    private final ReturnListener returnListener = (replyCode, replyText, exchange, routingKey, properties, body) -> LOG
            .error("Message returned! exchange=[" + exchange + "], routingKey=[" + routingKey + "]: replyCode=[" + replyCode
                    + "] replyMessage=[" + replyText + "]");

}
