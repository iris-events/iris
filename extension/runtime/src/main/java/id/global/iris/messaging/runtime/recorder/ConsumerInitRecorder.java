package id.global.iris.messaging.runtime.recorder;

import id.global.iris.messaging.runtime.consumer.ConsumerContainer;
import id.global.iris.messaging.runtime.consumer.FrontendEventConsumer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsumerInitRecorder {
    public void initConsumers(BeanContainer value) {
        value.instance(ConsumerContainer.class).initConsumers();
        value.instance(FrontendEventConsumer.class).initChannel();
    }
}
