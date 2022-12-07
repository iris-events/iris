package id.global.iris.messaging.runtime.recorder;

import id.global.iris.messaging.runtime.producer.ProducedEventExchangeDeclarator;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProducerDefinedExchangesRecorder {
    public void init(BeanContainer beanContainer) {
        beanContainer.instance(ProducedEventExchangeDeclarator.class).initExchanges();
    }
}
