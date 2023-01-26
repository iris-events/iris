package id.global.iris.messaging.runtime.exception;

import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.UUID;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.context.IrisContext;
import id.global.iris.messaging.runtime.requeue.MessageRequeueHandler;

class IrisExceptionHandlerTest {

    private Channel channel;

    private IrisExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        channel = Mockito.mock();
        MessageRequeueHandler messageRequeueHandler = Mockito.mock();
        TimestampProvider timestampProvider = Mockito.mock();
        exceptionHandler = new IrisExceptionHandler(new ObjectMapper(), new EventContext(), messageRequeueHandler,
                timestampProvider);
    }

    @Test
    void handleConstraintViolationException() throws Exception {
        final var message = Mockito.mock(Delivery.class);
        Mockito.when(message.getEnvelope()).thenReturn(new Envelope(1L, false, "exchange", "routing-key"));
        Mockito.when(message.getProperties()).thenReturn(new AMQP.BasicProperties());
        final var throwable = new ConstraintViolationException("test violation", Collections.emptySet());
        exceptionHandler.handleException(getIrisContext(), message, channel, throwable);

        Mockito.verify(channel, Mockito.times(1)).basicPublish(any(), any(), any(), any());
        Mockito.verify(channel, Mockito.times(1)).basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    @Test
    void handleInvalidFormatException() throws Exception {
        final var message = Mockito.mock(Delivery.class);
        Mockito.when(message.getEnvelope()).thenReturn(new Envelope(1L, false, "exchange", "routing-key"));
        Mockito.when(message.getProperties()).thenReturn(new AMQP.BasicProperties());
        final var jsonParser = new JsonFactory().createParser("{\"uuid\":\"notUuidFormat\"}");
        final var throwable = new InvalidFormatException(jsonParser, "not a UUID", "notUuidFormat", UUID.class);
        exceptionHandler.handleException(getIrisContext(), message, channel, throwable);

        Mockito.verify(channel, Mockito.times(1)).basicPublish(any(), any(), any(), any());
        Mockito.verify(channel, Mockito.times(1)).basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    private static IrisContext getIrisContext() {
        return new IrisContext();
    }
}