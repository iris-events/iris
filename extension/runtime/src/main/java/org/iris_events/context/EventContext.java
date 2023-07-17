package org.iris_events.context;

import static org.iris_events.common.MessagingHeaders.Message.SESSION_ID;
import static org.iris_events.common.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_RETRY_COUNT;

import java.util.Collections;
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

    private Optional<EventContextHolder> findEventContextHolder() {
        return Optional.ofNullable(eventContextThreadLocal.get());
    }

    private EventContextHolder getOrCreateEventContextHolder() {
        return findEventContextHolder().orElseGet(EventContextHolder::new);
    }

    public void setBasicProperties(AMQP.BasicProperties properties) {
        final var eventContextHolder = getOrCreateEventContextHolder();
        eventContextHolder.setAmqpBasicProperties(properties);
        this.eventContextThreadLocal.set(eventContextHolder);
    }

    public void setEnvelope(Envelope envelope) {
        final var eventContextHolder = getOrCreateEventContextHolder();
        eventContextHolder.setEnvelope(envelope);
        this.eventContextThreadLocal.set(eventContextHolder);
    }

    public AMQP.BasicProperties getAmqpBasicProperties() {
        return findEventContextHolder()
                .map(EventContextHolder::getAmqpBasicProperties)
                .orElse(null);
    }

    public Envelope getEnvelope() {
        return findEventContextHolder()
                .map(EventContextHolder::getEnvelope)
                .orElse(null);
    }

    public String getExchange() {
        return Optional.ofNullable(getEnvelope()).map(Envelope::getExchange).orElse(null);
    }

    public String getRoutingKey() {
        return Optional.ofNullable(getEnvelope()).map(Envelope::getRoutingKey).orElse(null);
    }

    public Map<String, Object> getHeaders() {
        return findEventContextHolder()
                .map(EventContextHolder::getAmqpBasicProperties)
                .map(AMQP.BasicProperties::getHeaders)
                .orElse(Collections.emptyMap());
    }

    public Optional<String> getCorrelationId() {
        return findEventContextHolder()
                .map(EventContextHolder::getAmqpBasicProperties)
                .map(AMQP.BasicProperties::getCorrelationId);
    }

    public Optional<String> getUserId() {
        return getHeaderValue(USER_ID);
    }

    public Optional<String> getSessionId() {
        return getHeaderValue(SESSION_ID);
    }

    public Integer getRetryCount() {
        return getHeaderValue(X_RETRY_COUNT).map(Integer::valueOf).orElse(0);
    }

    public Optional<String> getHeaderValue(final String header) {
        return findEventContextHolder()
                .map(EventContextHolder::getAmqpBasicProperties)
                .map(AMQP.BasicProperties::getHeaders)
                .filter(headers -> headers.containsKey(header))
                .map(headers -> headers.get(header))
                .map(Object::toString);
    }

    public void setSubscriptionId(final String subscriptionId) {
        setHeader(SUBSCRIPTION_ID, subscriptionId);
    }

    private void setHeader(final String key, final Object value) {
        final var basicProperties = findEventContextHolder()
                .map(EventContextHolder::getAmqpBasicProperties)
                .orElseThrow(() -> new IllegalStateException("AMQP.BasicProperties not set for the message context."));

        final var builder = basicProperties.builder();
        final var headers = new HashMap<>(basicProperties.getHeaders());
        headers.put(key, value);

        final var modifiedBasicProperties = builder.headers(headers).build();
        this.eventContextThreadLocal.get().setAmqpBasicProperties(modifiedBasicProperties);
    }

}
