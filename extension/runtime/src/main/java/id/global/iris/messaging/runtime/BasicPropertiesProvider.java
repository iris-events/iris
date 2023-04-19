package id.global.iris.messaging.runtime;

import static id.global.iris.common.constants.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.iris.common.constants.MessagingHeaders.Message.INSTANCE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.JWT;
import static id.global.iris.common.constants.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.ROUTER;
import static id.global.iris.common.constants.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static id.global.iris.common.constants.MessagingHeaders.Message.SESSION_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.USER_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.rabbitmq.client.AMQP;

import id.global.iris.common.annotations.Scope;
import id.global.iris.common.constants.DeliveryMode;
import id.global.iris.messaging.runtime.context.EventAppContext;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.producer.CorrelationIdProvider;
import id.global.iris.messaging.runtime.producer.RoutingDetails;

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

        final var eventName = routingDetails.eventName();
        final var scope = routingDetails.scope();
        final var userId = routingDetails.userId();
        final var sessionId = routingDetails.sessionId();

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

        final var subscriptionId = routingDetails.subscriptionId();
        if (subscriptionId != null) {
            headers.put(SUBSCRIPTION_ID, subscriptionId);
        }

        final var builder = basicProperties.builder();
        if (userId != null) {
            // when overriding user header, make sure, to clean possible existing event context properties
            builder.correlationId(correlationIdProvider.getCorrelationId());
            headers.put(ORIGIN_SERVICE_ID, serviceId);
            headers.remove(ROUTER);
            headers.remove(SESSION_ID);
            headers.put(USER_ID, userId);
        } else if (sessionId != null) {
            builder.correlationId(correlationIdProvider.getCorrelationId());
            headers.put(ORIGIN_SERVICE_ID, serviceId);
            headers.remove(ROUTER);
            headers.remove(USER_ID);
            headers.put(SESSION_ID, sessionId);
        }

        builder.deliveryMode(getDeliveryMode(routingDetails));

        return builder.headers(headers).build();
    }

    private int getDeliveryMode(final RoutingDetails routingDetails) {
        return routingDetails.persistent() ? DeliveryMode.PERSISTENT.getValue() : DeliveryMode.NON_PERSISTENT.getValue();
    }
}
