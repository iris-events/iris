package org.iris_events.runtime.recorder;

import org.iris_events.consumer.ConsumerContainer;
import org.iris_events.consumer.FrontendEventConsumer;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsumerInitRecorder {
    public void initConsumers(BeanContainer value) {
        value.beanInstance(ConsumerContainer.class).initConsumers();
        value.beanInstance(FrontendEventConsumer.class).initChannel();
    }
}
