package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = Logger.getLogger(AmqpProducer.class);
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Connection connection;
    private final ExecutorService pool;
    private final String hostName;
    private Optional<Channel> channel = Optional.empty();
    private AtomicInteger failCounter = new AtomicInteger(0);

    public final ThreadLocal<Channel> channels = new ThreadLocal<Channel>() {
        private static final Logger LOG = Logger.getLogger(ThreadLocal.class);

        @Override
        public Channel get() {
            return super.get();
        }
    };

    @Inject
    public AmqpProducer(AmqpConfiguration configuration, ObjectMapper objectMapper) {
        this.amqpConfiguration = configuration;
        this.objectMapper = objectMapper;
        this.hostName = getHostName();
        connect();
        //TODO: set pool according to needs (should this be in configuration?)
        pool = Executors.newFixedThreadPool(6);

    }

    private String getHostName() {
        String hostName;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            hostName = System.getenv("COMPUTERNAME");
        } else {
            hostName = System.getenv("HOSTNAME");
        }
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.error("Can't get hostname!", e);
            }
        }
        return hostName;
    }

    public AmqpConfiguration getAmqpConfiguration() {
        return amqpConfiguration;
    }

    boolean connected;

    private void connect() {

        if (this.connection != null)
            return;

        failCounter.set(0);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpConfiguration.getUrl());
        factory.setPort(amqpConfiguration.getPort());

        if (amqpConfiguration.isAuthenticated()) {
            factory.setUsername(amqpConfiguration.getUsername());
            factory.setPassword(amqpConfiguration.getPassword());
        }

        factory.setAutomaticRecoveryEnabled(true);

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
     * @param exchange exchange nam that message will be send to
     * @param routingKey optinal routingKey, if not provided, className of message send will be used
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishDirect(String exchange, Optional<String> routingKey, Object message, AMQP.BasicProperties properties)
            throws Exception {

        routingKey = routingKey.filter(s -> !s.isEmpty());
        byte[] bytes = new byte[0];

        bytes = objectMapper.writeValueAsBytes(message);
        publishMessage(exchange,
                routingKey.orElse(message.getClass().getSimpleName().toLowerCase()),
                properties,
                bytes);
    }

    /**
     * @param exchange exchange name where message will be send
     * @param topicRoutingKey topicRoutingKey for message routing example: log.internal.warn, log.external.warn,
     *        log.internal.error
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishTopic(String exchange, String topicRoutingKey, Object message, AMQP.BasicProperties properties)
            throws Exception {
        //TODO: maybe validate topic (routing key)
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
        publishMessage(exchange, topicRoutingKey, properties, bytes);
    }

    /**
     * @param fanoutExchange exchange name where message will be send
     * @param message Object/message to be send to exchange
     * @param properties additional properties for producer
     * @throws Exception
     */
    public void publishFanout(String fanoutExchange, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
        publishMessage(fanoutExchange, "", properties, bytes);
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
        byte[] bytes = new byte[0];

        bytes = objectMapper.writeValueAsBytes(message);
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
    public void publishFanoutAsync(String fanoutExchange, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
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
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
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
                        publish(exchange, routingKey, properties, bytes, existing);
                    } else {
                        final Channel newChannel = connection.createChannel();
                        newChannel.confirmSelect();
                        channels.set(newChannel);
                        publish(exchange, routingKey, properties, bytes, channels.get());

                    }
                } catch (Exception e) {
                    LOG.error("Message publishing failed!", e);
                }
            }, pool);
        } else {
            LOG.error("Connection is not open!");
            LOG.error("Message publishing failed!");
            connect();
        }

    }

    private void publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] bytes, Channel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicPublish(exchange, routingKey, properties, bytes);
            }
        } catch (Exception e) {
            LOG.error("Message publishing failed!", e);
        }
    }

    private Optional<Channel> createChannel() {
        try {

            return Optional.ofNullable(connection.createChannel());
        } catch (IOException e) {
            LOG.error("Failed to create channel!", e);
        }
        return Optional.empty();
    }

    private void publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] bytes,
            Optional<Channel> channel) {
        try {
            if (channel.isPresent()) {
                channel.get().basicPublish(exchange, routingKey, properties, bytes);
            }
        } catch (Exception e) {
            LOG.error("Message publishing failed!", e);
        }
    }

    private void publishMessage(final String exchange,
            final String routingKey,
            final AMQP.BasicProperties properties,
            final byte[] bytes) throws Exception {

        if (this.channel.isEmpty()) {
            this.channel = createChannel();
            this.channel.get().confirmSelect();

        }

        publish(exchange, routingKey, properties, bytes, this.channel);
    }

}
