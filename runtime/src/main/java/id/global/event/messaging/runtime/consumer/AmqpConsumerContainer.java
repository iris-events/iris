package id.global.event.messaging.runtime.consumer;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.ConnectionFactoryProvider;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;

@ApplicationScoped
public class AmqpConsumerContainer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpConsumerContainer.class);
    private Connection connection;
    private final ObjectMapper objectMapper;
    private final ConnectionFactoryProvider connectionFactoryProvider;
    private final String hostName;

    private int retryCount = 0;

    private final Map<String, AmqpConsumer> consumerMap;

    private final EventContext eventContext;

    @Inject
    public AmqpConsumerContainer(ConnectionFactoryProvider connectionFactoryProvider,
            ObjectMapper objectMapper, EventContext eventContext) {
        this.consumerMap = new HashMap<>();
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.hostName = Common.getHostName();
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
    }

    public void initConsumer() {
        ConnectionFactory factory = connectionFactoryProvider.getConnectionFactory();

        while (!createConnection(factory)) {
            retryCount++;
            if (retryCount >= 10) {
                break;
            }
            try {
                Thread.sleep(retryCount * 500L);
            } catch (InterruptedException e) {
                LOG.error("Wait for retry interrupted", e);
            }
        }
    }

    private boolean createConnection(ConnectionFactory factory) {
        try {
            connection = factory.newConnection("consumer_" + hostName);

            if (!consumerMap.isEmpty()) {
                consumerMap.forEach((queueName, consumer) -> {
                    try {
                        consumer.initChannel(connection);
                    } catch (IOException e) {
                        LOG.error("Exception initializing consumer channel", e);
                    }
                });
            }
            LOG.info("Consumer connected");
            return true;
        } catch (IOException | TimeoutException e) {
            LOG.error("Exception while initializing consumers", e);
            return false;
        }
    }

    public void addConsumer(MethodHandle methodHandle, MethodHandleContext methodHandleContext, AmqpContext amqpContext,
            Object eventHandlerInstance) {
        consumerMap.put(UUID.randomUUID().toString(), new AmqpConsumer(
                methodHandle,
                methodHandleContext,
                amqpContext,
                eventHandlerInstance,
                objectMapper,
                eventContext));
    }

    public int getNumOfConsumers() {
        return consumerMap.size();
    }

    public Connection getConnection() {
        return connection;
    }
}
