package id.global.event.messaging.runtime.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpProducer.class);
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Channel channel;
    private int failCounter = 0;

    public AmqpProducer(AmqpConfiguration configuration, ObjectMapper objectMapper) {
        this.amqpConfiguration = configuration;
        this.objectMapper = objectMapper;
        //        this.objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        connect();
    }

    public void connect() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpConfiguration.getUrl());
        factory.setPort(amqpConfiguration.getPort());

        if (amqpConfiguration.isAuthenticated()) {
            factory.setUsername(amqpConfiguration.getUsername());
            factory.setPassword(amqpConfiguration.getPassword());
        }
        factory.setAutomaticRecoveryEnabled(true);

        while (!createChannel(factory)) {
            if (failCounter >= 10)
                break;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean createChannel(ConnectionFactory connectionFactory) {
        try {
            LOG.info("Connecting to RabbitMQ");
            Connection connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            LOG.info("Connected to RabbitMQ");
        } catch (IOException | TimeoutException e) {
            LOG.error("Faild Connecton! Retry!", e);
            failCounter++;
            return false;
        }
        return true;
    }

    public void sendMessage(Object message) throws IOException {
        String objectClassName = message.getClass().getSimpleName();
        sendMessage(message, objectClassName);
    }

    public void sendMessage(Object message, String routingKey) throws IOException {
        sendMessage(message, "", routingKey);
    }

    public void sendMessage(Object message, String exchange, String routingKey) throws IOException {
        if (channel.isOpen()) {
            // TODO only for testing, queue declare should not happen on producer
            channel.queueDeclare(routingKey, false, false, false, null);
            byte[] bytes = objectMapper.writeValueAsBytes(message);
            channel.basicPublish(exchange, routingKey, null, bytes);
        }
    }

    // TODO remove when Vojko implements proper methods
    public AMQP.Exchange.DeclareOk declareFanoutExchange(String exchange) throws IOException {
        if (!channel.isOpen()) {
            LOG.warn("Could not declare exchange. Channel is closed.");
        } else {
            return channel.exchangeDeclare(exchange, "fanout");
        }
        return null;
    }

    public AMQP.Exchange.DeclareOk declareTopicExchange(String exchange) throws IOException {
        if (!channel.isOpen()) {
            LOG.warn("Could not declare exchange. Channel is closed.");
        } else {
            return channel.exchangeDeclare(exchange, "topic");
        }
        return null;
    }
}
