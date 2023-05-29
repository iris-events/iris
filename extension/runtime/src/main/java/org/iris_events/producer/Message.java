package org.iris_events.producer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

public record Message(Object message, RoutingDetails routingDetails, AMQP.BasicProperties properties, Envelope envelope) {
}
