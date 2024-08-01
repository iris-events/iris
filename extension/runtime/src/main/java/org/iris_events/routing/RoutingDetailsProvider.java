package org.iris_events.routing;

import static org.iris_events.common.Exchanges.BROADCAST;
import static org.iris_events.common.Exchanges.SESSION;
import static org.iris_events.common.Exchanges.USER;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.ExchangeTypeParser;
import org.iris_events.asyncapi.parsers.PersistentParser;
import org.iris_events.asyncapi.parsers.RoutingKeyParser;
import org.iris_events.context.EventContext;
import org.iris_events.exception.IrisSendException;
import org.iris_events.producer.RoutingDetails;
import org.iris_events.runtime.ExchangeNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RoutingDetailsProvider {
    private static final Logger log = LoggerFactory.getLogger(RoutingDetailsProvider.class);
    ExchangeNameProvider exchangeNameProvider;
    EventContext eventContext;

    @Inject
    public RoutingDetailsProvider(final ExchangeNameProvider exchangeNameProvider, final EventContext eventContext) {
        this.exchangeNameProvider = exchangeNameProvider;
        this.eventContext = eventContext;
    }

    public RoutingDetails getRoutingDetailsForClientScope(final org.iris_events.annotations.Message messageAnnotation,
            final Scope scope, final String userId) {

        final var exchange = Optional.ofNullable(userId)
                .map(u -> USER.getValue())
                .orElseGet(() -> switch (scope) {
                    case USER -> USER.getValue();
                    case SESSION -> SESSION.getValue();
                    case BROADCAST -> BROADCAST.getValue();
                    default -> throw new IrisSendException("Message scope " + scope + " not supported!");
                });

        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = String.format("%s.%s", eventName, exchange);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        final var builder = new RoutingDetails.Builder()
                .eventName(eventName)
                .exchange(exchange)
                .exchangeType(ExchangeType.TOPIC)
                .routingKey(routingKey)
                .scope(scope)
                // message should be sent to specific user (not necessarily the same as current user)
                .userId(userId)
                .persistent(persistent);

        return builder.build();
    }

    public RoutingDetails getRoutingDetailsFromAnnotation(final org.iris_events.annotations.Message messageAnnotation,
            final Scope scope, final String userId, final boolean propagate) {

        final var exchangeType = ExchangeTypeParser.getFromAnnotationClass(messageAnnotation);
        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = getRoutingKey(messageAnnotation, exchangeType);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        RoutingDetails.MiscRoutingDetailsBuilder routingDetailsBuilder = new RoutingDetails.Builder()
                .eventName(eventName)
                .exchange(eventName)
                .exchangeType(exchangeType)
                .routingKey(routingKey)
                .scope(scope)
                .userId(userId)
                .persistent(persistent)
                .propagate(propagate);
        return routingDetailsBuilder.build();
    }

    public RoutingDetails getRpcRoutingDetails(final org.iris_events.annotations.Message messageAnnotation,
            final String replyTo) {
        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        RoutingDetails.MiscRoutingDetailsBuilder routingDetailsBuilder = new RoutingDetails.Builder()
                .eventName(eventName)
                .exchange(exchangeNameProvider.getRpcResponseExchangeName(eventName))
                .exchangeType(ExchangeType.FANOUT) //TODO maybe try with topic, but i would go with fanout, and per service exchange, or maybe even DIRECT??
                .routingKey(replyTo)
                .scope(Scope.INTERNAL)
                .persistent(persistent)
                .propagate(true);
        final var routingDetails = routingDetailsBuilder.build();
        log.info(String.format("Built RPC routing details: %s", routingDetails.toString()));
        return routingDetails;
    }

    private String getRoutingKey(org.iris_events.annotations.Message messageAnnotation,
            final ExchangeType exchangeType) {
        if (exchangeType == ExchangeType.FANOUT) {
            return "";
        }

        return RoutingKeyParser.getFromAnnotationClass(messageAnnotation);
    }
}
