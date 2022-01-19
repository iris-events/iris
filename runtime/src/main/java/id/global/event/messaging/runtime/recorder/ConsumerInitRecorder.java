package id.global.event.messaging.runtime.recorder;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.consumer.FrontendAmqpConsumer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsumerInitRecorder {
    public void initConsumers(BeanContainer value) {
        value.instance(AmqpConsumerContainer.class).initConsumers();
        value.instance(FrontendAmqpConsumer.class).initChannel();
    }
}
