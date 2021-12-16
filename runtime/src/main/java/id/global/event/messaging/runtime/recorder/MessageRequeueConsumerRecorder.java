package id.global.event.messaging.runtime.recorder;

import java.io.IOException;

import id.global.event.messaging.runtime.requeue.MessageRequeueConsumer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MessageRequeueConsumerRecorder {
    public void registerMessageRequeueConsumer(final BeanContainer beanContainer) throws IOException {
        beanContainer.instance(MessageRequeueConsumer.class).initRetryConsumer();
    }
}
