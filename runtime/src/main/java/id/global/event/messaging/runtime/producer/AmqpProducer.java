package id.global.event.messaging.runtime.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.*;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpProducer.class);
    private final AmqpConfiguration amqpConfiguration;

    private final ObjectMapper objectMapper;
    private Channel channel;
    private Connection connection;
    private int failCounter = 0;
    private boolean isShutdown = true;

    public AmqpProducer(AmqpConfiguration configuration, ObjectMapper objectMapper) {
        this.amqpConfiguration = configuration;
        this.objectMapper = objectMapper;
        connect();
    }

    public Channel getChannel() {
        return channel;
    }

    public AmqpConfiguration getAmqpConfiguration() {
        return amqpConfiguration;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void connect() {

        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
            } catch (IOException e) {
                LOG.error("Problem with closing active connection", e);
            }
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpConfiguration.getUrl());
        factory.setPort(amqpConfiguration.getPort());


        if (amqpConfiguration.isAuthenticated()) {
            factory.setUsername(amqpConfiguration.getUsername());
            factory.setPassword(amqpConfiguration.getPassword());
        }
        factory.setAutomaticRecoveryEnabled(true);

        while (!createChannel(factory)) {
            if (failCounter >= 10) {
                //TODO: we need to set some flag that producer is not working
                isShutdown = true;
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error("Thread.sleep interupted!", e);
            }
        }
    }

    private boolean createChannel(ConnectionFactory connectionFactory) {
        try {
            LOG.info("Connecting to RabbitMQ");
            connection = connectionFactory.newConnection();

            channel = connection.createChannel();
            channel.addShutdownListener(o -> {
                LOG.error("Channel shutdown!");
                isShutdown = true;
            });
            channel.confirmSelect();
            isShutdown = false;
            LOG.info("Connected to RabbitMQ");
        } catch (IOException | TimeoutException e) {
            LOG.error("Faild Connecton! Count: " + failCounter++, e);
            return false;
        }
        return true;
    }


    public void publishFanout(String fanoutExchange, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
        publish(fanoutExchange, "", properties, bytes);
    }

    public void publishDirect(String exchange, String queue, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        String objectClassName = message.getClass().getSimpleName();
        bytes = objectMapper.writeValueAsBytes(message);
        publish(exchange, queue, properties, bytes);
    }

    public void publishExchange(String exchange, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
        publish(exchange, "", properties, bytes);
    }

    public void publishTopic(String exchange, String topic, Object message, AMQP.BasicProperties properties) throws Exception {
        byte[] bytes = new byte[0];
        bytes = objectMapper.writeValueAsBytes(message);
        publish(exchange, topic, properties, bytes);
    }

    public void publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] bytes) throws Exception {
        if (!channel.isOpen()) {
            throw new Exception("Channel is CLOSED!");
        }
        channel.basicPublish(exchange, routingKey, properties, bytes);

        if (!channel.isOpen()) {
            throw new Exception("Channel is CLOSED!");
        }
    }
}
