package org.iris_events.runtime.recorder;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import org.iris_events.consumer.ConsumerContainer;
import org.iris_events.context.IrisContext;
import org.iris_events.context.MethodHandleContext;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MethodHandleRecorder {

    public void registerConsumer(final BeanContainer beanContainer, MethodHandleContext methodHandleContext,
            IrisContext irisContext)
            throws NoSuchMethodException, IllegalAccessException, IOException {

        Object eventHandlerInstance = beanContainer.beanInstance(methodHandleContext.getHandlerClass());
        beanContainer.beanInstance(ConsumerContainer.class)
                .addConsumer(
                        createMethodHandle(methodHandleContext),
                        methodHandleContext,
                        irisContext,
                        eventHandlerInstance);
    }

    public void registerFrontendCallback(final BeanContainer beanContainer, MethodHandleContext methodHandleContext,
            IrisContext irisContext) throws NoSuchMethodException, IllegalAccessException, IOException {

        Object eventHandlerInstance = beanContainer.beanInstance(methodHandleContext.getHandlerClass());
        beanContainer.beanInstance(ConsumerContainer.class)
                .addFrontendCallback(createMethodHandle(methodHandleContext), methodHandleContext, irisContext,
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
