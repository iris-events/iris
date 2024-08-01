package org.iris_events.runtime;

import static org.iris_events.common.MessagingHeaders.Message.CACHE_TTL;
import static org.iris_events.common.MessagingHeaders.Message.CLIENT_VERSION;
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
import org.iris_events.exception.IrisSendException;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.RoutingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;

@ApplicationScoped
public class BasicPropertiesProvider {
    private static final Logger log = LoggerFactory.getLogger(BasicPropertiesProvider.class);
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
        final var correlationId = correlationIdProvider.getCorrelationId();
        log.debug("Creating new AMQP.BasicProperties with correlationId: {}", correlationId);
        return new AMQP.BasicProperties().builder()
                .correlationId(correlationId)
                // only set origin_service_id once and never break it as source cannot change
                .headers(Map.of(ORIGIN_SERVICE_ID, serviceId))
                .build();
    }

    private AMQP.BasicProperties buildAmqpBasicPropertiesWithCustomHeaders(final AMQP.BasicProperties basicProperties,
            final String serviceId, final RoutingDetails routingDetails) {

        final var eventName = routingDetails.getEventName();
        final var scope = routingDetails.getScope();
        final var userId = routingDetails.getUserId();
        final var propagate = routingDetails.getPropagate();
        final var subscriptionId = routingDetails.getSubscriptionId();
        final var cacheTtl = routingDetails.getCacheTtl();

        final var hostName = instanceInfoProvider.getInstanceName();
        final var headers = new HashMap<>(Optional.ofNullable(basicProperties.getHeaders()).orElse(new HashMap<>()));
        final var currentUserId = headers.get(USER_ID);
        headers.put(CURRENT_SERVICE_ID, serviceId);
        headers.put(INSTANCE_ID, hostName);
        headers.put(EVENT_TYPE, eventName);
        headers.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());
        Optional.ofNullable(subscriptionId).ifPresent(id -> headers.put(SUBSCRIPTION_ID, id));
        Optional.ofNullable(cacheTtl).ifPresent(ttl -> headers.put(CACHE_TTL, ttl));

        if (scope != Scope.INTERNAL) {
            // never propagate JWT when "leaving" backend
            headers.remove(JWT);
        }

        if (userId != null) {
            // message is being sent to specific user
            headers.put(USER_ID, userId);
            if (currentUserId != null && !currentUserId.equals(userId)) {
                // Message is being triggered by another users action. For such message this service is an origin.
                headers.put(ORIGIN_SERVICE_ID, serviceId);
                // do not propagate JWT in this case, as it no longer belongs to the user within message header
                headers.remove(JWT);
                // we should clean routing related headers as they will provide biased info to the router
                headers.remove(ROUTER);
                headers.remove(CLIENT_VERSION);
                headers.remove(SESSION_ID);
            }
        }

        if (scope == Scope.USER) {
            if (!headers.containsKey(USER_ID)) {
                throw new IrisSendException("Can not send USER scoped message without userId available from existing" +
                        " context or being provided as argument to send method.");
            }
            // make sure that none of router instances ignore the user scoped message
            // because user could have active sessions on multiple router instances
            headers.remove(ROUTER);
        }

        final var builder = basicProperties.builder();
        builder.deliveryMode(getDeliveryMode(routingDetails));

        if (!propagate) {
            // We should never reset correlationId automatically.
            // It should be done with care as it breaks correlation of events within backend.
            final var correlationId = correlationIdProvider.getCorrelationId();
            log.debug("CorrelationId propagation was purposely broken - resetting correlationId to: {}", correlationId);
            builder.correlationId(correlationId);
        }

        return builder.headers(headers).build();
    }

    private int getDeliveryMode(final RoutingDetails routingDetails) {
        return routingDetails.getPersistent() ? DeliveryMode.PERSISTENT.getValue() : DeliveryMode.NON_PERSISTENT.getValue();
    }
}
