package id.global.event.messaging.runtime;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsumerInitRecorder {
    public void initConsumers(BeanContainer value) {
        value.instance(AmqpConsumerContainer.class).initConsumers();
    }
}
