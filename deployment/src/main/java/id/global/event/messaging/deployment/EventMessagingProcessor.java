package id.global.event.messaging.deployment;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.common.annotations.EventMetadata;
import id.global.event.messaging.runtime.ConsumerInitRecorder;
import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.MethodHandleRecorder;
import id.global.event.messaging.runtime.channel.ConsumerChannelService;
import id.global.event.messaging.runtime.channel.ProducerChannelService;
import id.global.event.messaging.runtime.configuration.AmqpBuildConfiguration;
import id.global.event.messaging.runtime.connection.ConnectionFactoryProvider;
import id.global.event.messaging.runtime.connection.ConsumerConnectionProvider;
import id.global.event.messaging.runtime.connection.ProducerConnectionProvider;
import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.CorrelationIdProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class EventMessagingProcessor {

    public static class EventMessagingEnabled implements BooleanSupplier {

        AmqpBuildConfiguration config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    private static final String FEATURE = "event-messaging";
    private static final Logger LOG = LoggerFactory.getLogger(EventMessagingProcessor.class);

    @BuildStep(onlyIf = EventMessagingEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareAmqpBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer.produce(
                new AdditionalBeanBuildItem.Builder()
                        .addBeanClasses(
                                HostnameProvider.class,
                                ConsumerConnectionProvider.class,
                                ProducerConnectionProvider.class,
                                ConsumerChannelService.class,
                                ProducerChannelService.class,
                                AmqpConsumerContainer.class,
                                EventContext.class,
                                AmqpProducer.class,
                                ConnectionFactoryProvider.class,
                                CorrelationIdProvider.class,
                                EventMetadata.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }

    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void scanForMessageHandlers(CombinedIndexBuildItem index,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {
        MessageHandlerScanner scanner = new MessageHandlerScanner(index.getIndex());
        List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanner.scanMessageHandlerAnnotations();
        messageHandlerInfoBuildItems.forEach(messageHandlerProducer::produce);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovable() {
        // Any bean that has MyService in its set of bean types is considered unremovable
        return UnremovableBeanBuildItem.beanClassAnnotation("id.global.asyncapi.spec.annotations");
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void configureConsumer(final BeanContainerBuildItem beanContainer, ConsumerInitRecorder consumerInitRecorder,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems) {
        if (!messageHandlerInfoBuildItems.isEmpty()) {
            consumerInitRecorder.initConsumers(beanContainer.getValue());
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareMessageHandlers(final BeanContainerBuildItem beanContainer,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems,
            MethodHandleRecorder methodHandleRecorder) {
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        messageHandlerInfoBuildItems.forEach(col -> {
            try {
                Class<?> handlerClass = cl.loadClass(col.getDeclaringClass().name().toString());
                Class<?> eventClass = cl.loadClass(col.getParameterType().asClassType().name().toString());

                MethodHandleContext methodHandleContext = new MethodHandleContext(handlerClass, eventClass,
                        col.getMethodName());
                AmqpContext amqpContext = new AmqpContext(col.getQueue(), col.getExchange(), col.getBindingKeys(),
                        col.getExchangeType());

                LOG.info("Registering handler. Handler class = " + handlerClass.getName() +
                        " eventClass = " + eventClass.getName() +
                        " methodName = " + col.getMethodName());

                methodHandleRecorder.registerConsumer(beanContainer.getValue(), methodHandleContext, amqpContext);

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IOException e) {
                LOG.error("Could not record method handle", e);
            }
        });
    }
}
