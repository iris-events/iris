package org.iris_events.runtime.producer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.iris_events.producer.ExchangeDeclarator;
import org.iris_events.producer.ProducedEventExchangeInitializer;
import org.iris_events.runtime.channel.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ProducedEventExchangeInitializerTest {

    ProducedEventExchangeInitializer producedEventExchangeInitializer;
    ChannelService channelServiceMock;
    ExchangeDeclarator exchangeDeclarator;

    @BeforeEach
    public void setup() {
        channelServiceMock = Mockito.mock(ChannelService.class);
        exchangeDeclarator = Mockito.mock(ExchangeDeclarator.class);
        producedEventExchangeInitializer = new ProducedEventExchangeInitializer(exchangeDeclarator);
    }

    @Test
    void addProducerDefinedExchange() {
        producedEventExchangeInitializer.addProducerDefinedExchange("exchangename", ExchangeType.FANOUT, Scope.INTERNAL);
        final var producerDefinedExchanges = producedEventExchangeInitializer.getProducerDefinedExchanges();

        assertThat(producerDefinedExchanges, is(notNullValue()));
        assertThat(producerDefinedExchanges.size(), is(1));
        assertThat(producerDefinedExchanges.get(0).exchangeName(), is("exchangename"));
        assertThat(producerDefinedExchanges.get(0).type(), is(ExchangeType.FANOUT));
        assertThat(producerDefinedExchanges.get(0).scope(), is(Scope.INTERNAL));
    }

    @Test
    void initExchanges() throws IOException {
        final var exchangeNameCaptor = ArgumentCaptor.forClass(String.class);
        final var exchangeTypeCaptor = ArgumentCaptor.forClass(ExchangeType.class);
        final var isFrontendCaptor = ArgumentCaptor.forClass(Boolean.class);

        producedEventExchangeInitializer.addProducerDefinedExchange("exchangename1", ExchangeType.FANOUT, Scope.INTERNAL);
        producedEventExchangeInitializer.addProducerDefinedExchange("exchangename2", ExchangeType.FANOUT, Scope.INTERNAL);
        producedEventExchangeInitializer.addProducerDefinedExchange("exchangename3", ExchangeType.FANOUT, Scope.FRONTEND);

        producedEventExchangeInitializer.initExchanges();

        Mockito.doNothing().when(exchangeDeclarator).declareExchange(anyString(), any(ExchangeType.class), anyBoolean());
        Mockito.verify(exchangeDeclarator, Mockito.times(3))
                .declareExchange(exchangeNameCaptor.capture(), exchangeTypeCaptor.capture(), isFrontendCaptor.capture());

        final var exchangeNameValues = exchangeNameCaptor.getAllValues();
        final var exchangeTypeValues = exchangeTypeCaptor.getAllValues();
        final var isFrontendValues = isFrontendCaptor.getAllValues();

        assertThat(exchangeNameValues.size(), is(3));
        assertThat(exchangeTypeValues.size(), is(3));
        assertThat(isFrontendValues.size(), is(3));

        assertThat(exchangeNameValues.get(0), is("exchangename1"));
        assertThat(exchangeNameValues.get(1), is("exchangename2"));
        assertThat(exchangeNameValues.get(2), is("exchangename3"));

        assertThat(exchangeTypeValues.get(0), is(ExchangeType.FANOUT));
        assertThat(exchangeTypeValues.get(1), is(ExchangeType.FANOUT));
        assertThat(exchangeTypeValues.get(2), is(ExchangeType.FANOUT));

        assertThat(isFrontendValues.get(0), is(false));
        assertThat(isFrontendValues.get(1), is(false));
        assertThat(isFrontendValues.get(2), is(true));
    }
}
