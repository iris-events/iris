package id.global.event.messaging.runtime;

import org.jboss.logging.Logger;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsumerConfigRecorder {
    private static final Logger LOG = Logger.getLogger(ConsumerConfigRecorder.class);

    public void initConsumerConfig(BeanContainer value) {
        value.instance(AmqpConsumerContainer.class).initConsumer();
    }
}