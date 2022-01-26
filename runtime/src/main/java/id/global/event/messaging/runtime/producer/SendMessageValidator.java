package id.global.event.messaging.runtime.producer;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.runtime.exception.AmqpSendException;

class SendMessageValidator {
    static void validate(String exchange, String routingKey, ExchangeType exchangeType) throws AmqpSendException {
        validateExchangePresent(exchange);
        validateRoutingKey(routingKey, exchangeType);
    }

    private static void validateExchangePresent(String exchange) throws AmqpSendException {
        if (isNullOrEmpty(exchange)) {
            throw new AmqpSendException("Can not publish message to empty exchange");
        }
    }

    private static void validateRoutingKey(String routingKey, ExchangeType exchangeType) throws AmqpSendException {
        if (exchangeType.equals(TOPIC) || exchangeType.equals(DIRECT)) {
            if (isNullOrEmpty(routingKey)) {
                throw new AmqpSendException(
                        String.format("Can not publish message to %s exchange with empty routing key.", exchangeType));
            }
        }
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }
}