package id.global.iris.messaging.runtime.producer;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static id.global.iris.common.annotations.ExchangeType.TOPIC;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.messaging.runtime.exception.IrisSendException;

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
