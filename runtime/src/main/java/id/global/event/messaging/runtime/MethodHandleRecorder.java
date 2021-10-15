package id.global.event.messaging.runtime;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MethodHandleRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandleRecorder.class.getName());

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
