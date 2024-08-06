package org.iris_events.context;

import static org.iris_events.common.MessagingHeaders.Message.SESSION_ID;
import static org.iris_events.common.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_RETRY_COUNT;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

@ApplicationScoped
public class EventContext {
    private final ThreadLocal<EventContextHolder> eventContextThreadLocal;

    public EventContext() {
        this.eventContextThreadLocal = new ThreadLocal<>();
        this.eventContextThreadLocal.set(new EventContextHolder());
    }

    private EventContextHolder getOrCreateEventContextHolder() {
        EventContextHolder eventContextHolder = eventContextThreadLocal.get();
        if (eventContextHolder != null) {
            return eventContextHolder;
        }
        return new EventContextHolder();
    }

    public void setBasicProperties(AMQP.BasicProperties properties) {
        final EventContextHolder eventContextHolder = getOrCreateEventContextHolder();
        eventContextHolder.setAmqpBasicProperties(properties);
        this.eventContextThreadLocal.set(eventContextHolder);
    }

    public void setEnvelope(Envelope envelope) {
        final EventContextHolder eventContextHolder = getOrCreateEventContextHolder();
        eventContextHolder.setEnvelope(envelope);
        this.eventContextThreadLocal.set(eventContextHolder);
    }

    public AMQP.BasicProperties getAmqpBasicProperties() {
        EventContextHolder eventContextHolder = this.eventContextThreadLocal.get();
        if (eventContextHolder == null) {
            return null;
        }
        return eventContextHolder.getAmqpBasicProperties();
    }

    public Envelope getEnvelope() {
        final EventContextHolder eventContextHolder = this.eventContextThreadLocal.get();
        if (eventContextHolder != null) {
            return eventContextHolder.getEnvelope();
        }
        return null;
    }

    public String getExchange() {
        return Optional.ofNullable(getEnvelope()).map(Envelope::getExchange).orElse(null);
    }

    public String getRoutingKey() {
        return Optional.ofNullable(getEnvelope()).map(Envelope::getRoutingKey).orElse(null);
    }

    public Map<String, Object> getHeaders() {
        final AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties();
        if (amqpBasicProperties == null || amqpBasicProperties.getHeaders() == null) {
            return Map.of();
        }

        return amqpBasicProperties.getHeaders();
    }

    public String getCorrelationId() {
        final AMQP.BasicProperties amqpBasicProperties = getProperties();
        if (amqpBasicProperties == null) {
            return null;
        }

        return amqpBasicProperties.getCorrelationId();
    }

    private AMQP.BasicProperties getProperties() {
        final EventContextHolder eventContextHolder = this.eventContextThreadLocal.get();
        if (eventContextHolder == null) {
            return null;
        }

        return eventContextHolder.getAmqpBasicProperties();
    }

    public Optional<String> getUserId() {
        return getHeaderValue(USER_ID);
    }

    public Optional<String> getMessageId() {
        return Optional.ofNullable(getAmqpBasicProperties().getMessageId());
    }

    public Optional<String> getSessionId() {
        return getHeaderValue(SESSION_ID);
    }

    public Integer getRetryCount() {
        return getHeaderValue(X_RETRY_COUNT).map(Integer::valueOf).orElse(0);
    }

    public Optional<String> getHeaderValue(final String header) {
        final AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties();
        if (amqpBasicProperties == null) {
            return Optional.empty();
        }

        final Object headerValue = amqpBasicProperties.getHeaders().get(header);
        if (headerValue == null) {
            return Optional.empty();
        }

        return Optional.of(headerValue.toString());
    }

    public void setSubscriptionId(final String subscriptionId) {
        setHeader(SUBSCRIPTION_ID, subscriptionId);
    }

    public void setHeader(final String key, final Object value) {
        final AMQP.BasicProperties basicProperties = getAmqpBasicProperties();
        if (basicProperties == null) {
            throw new IllegalStateException("AMQP.BasicProperties not set for the message context.");
        }

        final AMQP.BasicProperties.Builder builder = basicProperties.builder();
        final HashMap<String, Object> headers = new HashMap<>(basicProperties.getHeaders());
        headers.put(key, value);

        final AMQP.BasicProperties modifiedBasicProperties = builder.headers(headers).build();
        this.eventContextThreadLocal.get().setAmqpBasicProperties(modifiedBasicProperties);
    }

}
