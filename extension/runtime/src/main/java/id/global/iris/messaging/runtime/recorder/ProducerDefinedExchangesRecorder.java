package id.global.iris.messaging.runtime.recorder;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;
import id.global.iris.messaging.runtime.producer.ProducedEventExchangeDeclarator;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProducerDefinedExchangesRecorder {
    public void init(BeanContainer beanContainer) {
        beanContainer.instance(ProducedEventExchangeDeclarator.class).initExchanges();
    }

    public void registerProducerDefinedExchange(final BeanContainer beanContainer, final String exchange, final ExchangeType exchangeType, final
            Scope scope) {
        beanContainer.instance(ProducedEventExchangeDeclarator.class).addProducerDefinedExchange(exchange, exchangeType, scope);
    }
}
