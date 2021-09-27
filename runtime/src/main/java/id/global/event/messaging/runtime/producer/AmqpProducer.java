package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.common.annotations.EventMetadata;
import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.EventContext;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = Logger.getLogger(AmqpProducer.class.getName());
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Connection connection;
    private final String hostName;

    private final ConcurrentHashMap<String, Channel> regularChannels = new ConcurrentHashMap<>();

    private final AtomicInteger failCounter = new AtomicInteger(0);
    private final Object lock = new Object();
    private final AtomicInteger count = new AtomicInteger(0);
    private boolean connected;
    private final EventContext eventContext;
    private final long waitTimeout = 2000;

    @Inject
    public AmqpProducer(AmqpConfiguration configuration, ObjectMapper objectMapper, EventContext eventContext) {
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

    public boolean publish(Object message) {
        EventMetadata m = message.getClass().getAnnotation(EventMetadata.class);

        publish(m.exchange(),
                Optional.of(m.routingKey()),
                ExchangeType.valueOf(m.exchangeType().toUpperCase()),
                message, false);

        return true;
    }

    /**
     * @param exchange exchange name to which we send message to
     * @param routingKey routing key to route message to queue
     * @param type exchange type (DIRECT, FANOUT, TOPIC)
     * @param message message to be send
     * @param failImmediately fail immediately on publishing error
     * @return true/false if message was published successfult to broker
     */
    public boolean publish(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            boolean failImmediately) {
        AMQP.BasicProperties amqpBasicProperties = eventContext.getAmqpBasicProperties();
        return routePublish(exchange, routingKey, type, message, failImmediately, amqpBasicProperties);
    }

    /**
     * @param exchange exchange name to which we send message to
     * @param routingKey routing key to route message to queue
     * @param type exchange type (DIRECT, FANOUT, TOPIC)
     * @param message message to be send
     * @param failImmediately fail immediately on publishing error
     * @param properties BasicProperties for publish
     * @return true/false if message was published successfult to broker
     */
    public boolean publish(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            boolean failImmediately, AMQP.BasicProperties properties) {
        final var amqpBasicProperties = Optional.ofNullable(properties).orElse(this.eventContext.getAmqpBasicProperties());
        return routePublish(exchange, routingKey, type, message, failImmediately, amqpBasicProperties);
    }

    private boolean routePublish(String exchange, Optional<String> routingKey, ExchangeType type, Object message,
            boolean failImmediately, AMQP.BasicProperties properties) {
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

    private void publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] bytes,
            Optional<Channel> channel) throws Exception {
        // TODO handle optional
        channel.get().basicPublish(exchange, routingKey, true, properties, bytes);

    }

    private Channel createChannel() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            LOG.error("Failed to create channel!", e);
        }
        return null;
    }

    public void addReturnListener(String channelKey, ReturnListener returnListener, ReturnCallback returnCallback) {
        Channel c = getChannel(channelKey);
        if (c != null) {
            c.clearReturnListeners();
            if (returnListener != null)
                c.addReturnListener(returnListener);
            if (returnCallback != null)
                c.addReturnListener(returnCallback);
        } else {
            LOG.error("Cannot add return listeners as channel does not exist! channelKey={" + channelKey + "}");
        }
    }

    public void addConfirmListeners(String channelKey, ConfirmListener confirmListener) {
        Channel c = getChannel(channelKey);
        if (c != null) {
            if (confirmListener != null) {
                c.clearConfirmListeners();
                c.addConfirmListener(confirmListener);
            }
        } else {
            LOG.error("Cannot add confirm listeners as channel does not exist! channelKey={" + channelKey + "}");
        }
    }

    private Channel getChannel(String channelKey) {
        if (regularChannels.get(channelKey) != null)
            return regularChannels.get(channelKey);
        try {
            createChannel(channelKey, true);
        } catch (IOException ignored) {
        }
        return regularChannels.get(channelKey);
    }

    private void createChannel(String channelKey, boolean withConfirmations) throws IOException {
        Channel ch = createChannel();
        if (ch != null) {
            if (withConfirmations) {
                ch.confirmSelect();
            }
            regularChannels.put(channelKey, ch);
        }
    }

    private boolean publishMessage(final String exchange,
            final String routingKey,
            final AMQP.BasicProperties properties,
            final byte[] bytes, boolean immediate) {
        synchronized (this.lock) {

            if (this.connection.isOpen()) {
                String channelKey = exchange + "_" + routingKey;
                try {
                    Channel existingChannel = getChannel(channelKey);
                    if (existingChannel == null) {
                        createChannel(channelKey, true);
                        existingChannel = getChannel(channelKey);
                    }

                    publish(exchange, routingKey, properties, bytes, Optional.of(existingChannel));

                    if (count.incrementAndGet() == 100 || immediate) { //for every 100 messages that are send wait for all confirmations
                        existingChannel.waitForConfirms(waitTimeout); //timeout is 500ms (should we change this to longer?
                        count.set(0);
                    }
                    return true;
                } catch (Exception e) {
                    regularChannels.remove(channelKey);
                    return false;
                }
            } else {
                return false;
            }
        }
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
