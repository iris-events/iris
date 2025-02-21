package org.iris_events.runtime;

import static org.iris_events.common.MessagingHeaders.Message.CACHE_TTL;
import static org.iris_events.common.MessagingHeaders.Message.CLIENT_VERSION;
import static org.iris_events.common.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;
import static org.iris_events.common.MessagingHeaders.Message.INSTANCE_ID;
import static org.iris_events.common.MessagingHeaders.Message.JWT;
import static org.iris_events.common.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.REQUEST_ID;
import static org.iris_events.common.MessagingHeaders.Message.ROUTER;
import static org.iris_events.common.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static org.iris_events.common.MessagingHeaders.Message.SESSION_ID;
import static org.iris_events.common.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;

import java.util.HashMap;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.iris_events.annotations.Scope;
import org.iris_events.common.DeliveryMode;
import org.iris_events.context.EventContext;
import org.iris_events.exception.IrisSendException;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.RoutingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

@ApplicationScoped
public class BasicPropertiesProvider {
    private static final Logger log = LoggerFactory.getLogger(BasicPropertiesProvider.class);

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

    @Context
    HttpHeaders httpHeaders;

    /**
     * Gets AmqpBasicProperties from eventContext or creates new, if eventContext returns null. Basic properties headers are
     * modified according to provided routingDetails.
     * <p>
     * If provided RoutingDetails.Scope is INTERNAL, existing JWT token will be removed from the headers.
     * RoutingDetails.UserId and RoutingDetails.SessionId are mutually exclusive
     *
     * @param routingDetails routingDetails that dictate header generation in properties
     * @return AmqpBasicProperties
     */
    @ActivateRequestContext
    public AMQP.BasicProperties getOrCreateAmqpBasicProperties(final RoutingDetails routingDetails) {
        final String serviceId = eventAppInfoProvider.getApplicationId();
        AMQP.BasicProperties basicProperties = eventContext.getAmqpBasicProperties();
        if (basicProperties == null) {
            log.debug("No basic properties found within eventContext - building new one.");
            basicProperties = createAmqpBasicProperties(serviceId);
        }

        return buildAmqpBasicPropertiesWithCustomHeaders(basicProperties, serviceId, routingDetails);
    }

    private AMQP.BasicProperties createAmqpBasicProperties(final String serviceId) {
        // when thread was started by HTTP request we check for x-request-id header and use it as a correlation id
        var activeRequest = Arc.container().instance(CurrentVertxRequest.class).get();

        var correlationId = Optional.ofNullable(activeRequest.getCurrent())
                .map(ctx -> httpHeaders.getHeaderString(REQUEST_ID))
                .orElseGet(correlationIdProvider::getCorrelationId);

        log.debug("Creating new AMQP.BasicProperties with correlationId: {}", correlationId);
        final var builder = new AMQP.BasicProperties().builder()
                .correlationId(correlationId);
        final var headers = new HashMap<String, Object>();
        // only set origin_service_id once and never break it as source cannot change
        Optional.ofNullable(serviceId).ifPresent(id -> headers.put(ORIGIN_SERVICE_ID, serviceId));
        builder.headers(headers);

        return builder.build();
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
        Optional.ofNullable(serviceId).ifPresent(id -> headers.put(CURRENT_SERVICE_ID, serviceId));
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
