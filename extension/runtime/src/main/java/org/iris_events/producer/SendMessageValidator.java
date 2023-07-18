package org.iris_events.producer;

import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.iris_events.annotations.ExchangeType.TOPIC;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.exception.IrisSendException;

class SendMessageValidator {
    static void validate(final RoutingDetails routingDetails) throws IrisSendException {
        final var exchange = routingDetails.getExchange();
        final var exchangeType = routingDetails.getExchangeType();
        final var routingKey = routingDetails.getRoutingKey();

        validateExchangePresent(exchange);
        validateRoutingKey(routingKey, exchangeType);
    }

    private static void validateExchangePresent(String exchange) throws IrisSendException {
        if (isNullOrEmpty(exchange)) {
            throw new IrisSendException("Can not publish message to empty exchange");
        }
    }

    private static void validateRoutingKey(String routingKey, ExchangeType exchangeType) throws IrisSendException {
        if (exchangeType.equals(TOPIC) || exchangeType.equals(DIRECT)) {
            if (isNullOrEmpty(routingKey)) {
                throw new IrisSendException(
                        String.format("Can not publish message to %s exchange with empty routing key.", exchangeType));
            }
        }
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }
}
