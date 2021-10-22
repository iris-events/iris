package id.global.event.messaging.runtime.producer;

import com.rabbitmq.client.AMQP;

public record Message(Object message, String exchange, String routingKey, AMQP.BasicProperties properties) {
}
