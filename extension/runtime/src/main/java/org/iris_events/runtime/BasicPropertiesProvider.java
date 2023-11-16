package org.iris_events.runtime;

import static org.iris_events.common.MessagingHeaders.Message.CACHE_TTL;
import static org.iris_events.common.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;
import static org.iris_events.common.MessagingHeaders.Message.INSTANCE_ID;
import static org.iris_events.common.MessagingHeaders.Message.JWT;
import static org.iris_events.common.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.ROUTER;
import static org.iris_events.common.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static org.iris_events.common.MessagingHeaders.Message.SESSION_ID;
import static org.iris_events.common.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.annotations.Scope;
import org.iris_events.common.DeliveryMode;
import org.iris_events.context.EventAppContext;
import org.iris_events.context.EventContext;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.RoutingDetails;

import com.rabbitmq.client.AMQP;

@ApplicationScoped
public class BasicPropertiesProvider {
    public static final String SERVICE_ID_UNAVAILABLE_FALLBACK = "N/A";

    @Inject
    EventAppInfoProvider eventAppInfoProvider;

    @Inject
    EventContext eventContext;

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Inject
    InstanceInfoProvider instanceInfoProvider;

    @Inject
    TimestampProvider timestampProvider;

    /**
     * Gets AmqpBasicProperties from eventContext or creates new, if eventContext returns null. Basic properties headers are
     * modified according to provided routingDetails.
     *
     * If provided RoutingDetails.Scope is INTERNAL, existing JWT token will be removed from the headers.
     * RoutingDetails.UserId and RoutingDetails.SessionId are mutually exclusive
     *
     * @param routingDetails routingDetails that dictate header generation in properties
     * @return AmqpBasicProperties
     */
    public AMQP.BasicProperties getOrCreateAmqpBasicProperties(final RoutingDetails routingDetails) {
        final var eventAppContext = Optional.ofNullable(eventAppInfoProvider.getEventAppContext());
        final var serviceId = eventAppContext.map(EventAppContext::getId).orElse(SERVICE_ID_UNAVAILABLE_FALLBACK);
        final var basicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(createAmqpBasicProperties(serviceId));

        return buildAmqpBasicPropertiesWithCustomHeaders(basicProperties, serviceId, routingDetails);
    }

    private AMQP.BasicProperties createAmqpBasicProperties(final String serviceId) {
        return new AMQP.BasicProperties().builder()
                .correlationId(correlationIdProvider.getCorrelationId())
                .headers(Map.of(ORIGIN_SERVICE_ID, serviceId))
                .build();
    }

    private AMQP.BasicProperties buildAmqpBasicPropertiesWithCustomHeaders(final AMQP.BasicProperties basicProperties,
            final String serviceId, final RoutingDetails routingDetails) {

        final var eventName = routingDetails.getEventName();
        final var scope = routingDetails.getScope();
        final var userId = routingDetails.getUserId();
        final var sessionId = routingDetails.getSessionId();
        final var propagate = routingDetails.getPropagate();

        final var hostName = instanceInfoProvider.getInstanceName();
        final var headers = new HashMap<>(basicProperties.getHeaders());
        headers.put(CURRENT_SERVICE_ID, serviceId);
        headers.put(INSTANCE_ID, hostName);
        headers.put(EVENT_TYPE, eventName);
        headers.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());
        if (scope != Scope.INTERNAL) {
            // never propagate JWT when "leaving" backend
            headers.remove(JWT);
        }

        final var subscriptionId = routingDetails.getSubscriptionId();
        if (subscriptionId != null) {
            headers.put(SUBSCRIPTION_ID, subscriptionId);
        }
        final var cacheTtl = routingDetails.getCacheTtl();
        if (cacheTtl != null) {
            headers.put(CACHE_TTL, cacheTtl);
        }

        final var builder = basicProperties.builder();
        if (userId != null || sessionId != null) {
            builder.correlationId(correlationIdProvider.getCorrelationId());
            headers.put(ORIGIN_SERVICE_ID, serviceId);
            headers.remove(ROUTER);

            if (userId != null) {
                headers.put(USER_ID, userId);
            }

            if (sessionId != null) {
                headers.put(SESSION_ID, sessionId);
            }
        }

        if (!propagate) {
            builder.correlationId(correlationIdProvider.getCorrelationId());
        }

        builder.deliveryMode(getDeliveryMode(routingDetails));

        return builder.headers(headers).build();
    }

    private int getDeliveryMode(final RoutingDetails routingDetails) {
        return routingDetails.getPersistent() ? DeliveryMode.PERSISTENT.getValue() : DeliveryMode.NON_PERSISTENT.getValue();
    }
}
