package org.iris_events.runtime.producer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.producer.ExchangeDeclarator;
import org.iris_events.runtime.channel.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;

class ExchangeDeclaratorTest {

    private Channel channel;
    private ChannelService channelService;
    private ExchangeDeclarator exchangeDeclarator;

    @BeforeEach
    public void setup() {
        this.channelService = Mockito.mock(ChannelService.class);
        this.channel = Mockito.mock(Channel.class);
        this.exchangeDeclarator = new ExchangeDeclarator(channelService);
    }

    @Test
    void declareExchange() throws IOException {
        Mockito.when(channelService.getOrCreateChannelById(Mockito.anyString())).thenReturn(this.channel);
        Mockito.when(channel.exchangeDeclare(Mockito.anyString(), Mockito.any(BuiltinExchangeType.class),
                Mockito.anyBoolean())).thenReturn(new AMQImpl.Exchange.DeclareOk());

        exchangeDeclarator.declareExchange("exchangename", ExchangeType.FANOUT, false);

        final var exchangeNameCaptor = ArgumentCaptor.forClass(String.class);
        final var builtinExchangeTypeCaptor = ArgumentCaptor.forClass(BuiltinExchangeType.class);
        final var durableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(channel, Mockito.times(1))
                .exchangeDeclare(exchangeNameCaptor.capture(), builtinExchangeTypeCaptor.capture(), durableCaptor.capture());

        final var exchangeName = exchangeNameCaptor.getValue();
        final var builtinExchangeType = builtinExchangeTypeCaptor.getValue();
        final var durable = durableCaptor.getValue();

        assertThat(exchangeName, is("exchangename"));
        assertThat(builtinExchangeType, is(BuiltinExchangeType.FANOUT));
        assertThat(durable, is(true));
    }

    @Test
    void declareFrontendExchange() throws IOException {
        Mockito.when(channelService.getOrCreateChannelById(Mockito.anyString())).thenReturn(this.channel);
        Mockito.when(channel.exchangeDeclare(Mockito.anyString(), Mockito.any(BuiltinExchangeType.class),
                Mockito.anyBoolean())).thenReturn(new AMQImpl.Exchange.DeclareOk());

        exchangeDeclarator.declareExchange("exchangename", ExchangeType.FANOUT, true);

        final var exchangeNameCaptor = ArgumentCaptor.forClass(String.class);
        final var builtinExchangeTypeCaptor = ArgumentCaptor.forClass(BuiltinExchangeType.class);
        final var durableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(channel, Mockito.times(1))
                .exchangeDeclare(exchangeNameCaptor.capture(), builtinExchangeTypeCaptor.capture(), durableCaptor.capture());

        final var exchangeName = exchangeNameCaptor.getValue();
        final var builtinExchangeType = builtinExchangeTypeCaptor.getValue();
        final var durable = durableCaptor.getValue();

        assertThat(exchangeName, is("exchangename"));
        assertThat(builtinExchangeType, is(BuiltinExchangeType.TOPIC));
        assertThat(durable, is(false));
    }
}
