package id.global.event.messaging.runtime.recorder;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MethodHandleRecorder {

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

        final var publicLookup = MethodHandles.publicLookup();
        final var optionalReturnEventClass = Optional.ofNullable(methodHandleContext.getReturnEventClass());
        final var returnType = optionalReturnEventClass.isPresent() ? optionalReturnEventClass.get() : void.class;
        final var methodType = MethodType.methodType(returnType, methodHandleContext.getEventClass());

        return publicLookup.findVirtual(methodHandleContext.getHandlerClass(), methodHandleContext.getMethodName(), methodType);
    }

}
