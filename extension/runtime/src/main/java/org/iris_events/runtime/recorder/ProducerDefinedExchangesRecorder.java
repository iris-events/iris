package org.iris_events.runtime.recorder;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.iris_events.producer.ProducedEventExchangeInitializer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProducerDefinedExchangesRecorder {
    public void init(BeanContainer beanContainer) {
        beanContainer.instance(ProducedEventExchangeInitializer.class).initExchanges();
    }

    public void registerProducerDefinedExchange(final BeanContainer beanContainer, final String exchange, final ExchangeType exchangeType, final
            Scope scope) {
        beanContainer.instance(ProducedEventExchangeInitializer.class).addProducerDefinedExchange(exchange, exchangeType, scope);
    }
}
