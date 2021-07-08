package id.global.event.messaging.runtime;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Recorder
public class MethodHandleRecorder {
    private static final Logger LOG = Logger.getLogger(MethodHandleRecorder.class);

    public void registerConsumer(final BeanContainer beanContainer, MethodHandleContext methodHandleContext,
            AmqpContext amqpContext)
            throws NoSuchMethodException, IllegalAccessException, IOException {

        Object eventHandlerInstance = beanContainer.instance(methodHandleContext.getHandlerClass());
        beanContainer.instance(AmqpConsumerContainer.class)
                .addConsumer(
                        createMethodHandle(methodHandleContext),
                        methodHandleContext,
                        amqpContext,
                        eventHandlerInstance);
    }

    private MethodHandle createMethodHandle(MethodHandleContext methodHandleContext)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        // currently only void return public methods
        MethodType methodType = MethodType.methodType(void.class, methodHandleContext.getEventClass());
        return publicLookup.findVirtual(methodHandleContext.getHandlerClass(), methodHandleContext.getMethodName(), methodType);
    }

}
