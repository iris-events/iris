package id.global.iris.messaging.runtime.recorder;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;
import id.global.iris.messaging.runtime.producer.ProducedEventExchangeInitializer;
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
