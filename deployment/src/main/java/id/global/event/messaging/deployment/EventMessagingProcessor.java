package id.global.event.messaging.deployment;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.event.messaging.deployment.scanner.EventAppScanner;
import id.global.event.messaging.deployment.scanner.MessageHandlerScanner;
import id.global.event.messaging.runtime.EventAppInfoProvider;
import id.global.event.messaging.runtime.InstanceInfoProvider;
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
import id.global.event.messaging.runtime.recorder.ConsumerInitRecorder;
import id.global.event.messaging.runtime.recorder.EventAppRecorder;
import id.global.event.messaging.runtime.recorder.MessageRequeueConsumerRecorder;
import id.global.event.messaging.runtime.recorder.MethodHandleRecorder;
import id.global.event.messaging.runtime.requeue.MessageRequeueConsumer;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;
import id.global.event.messaging.runtime.requeue.RetryQueues;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

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

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareAmqpBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer.produce(
                new AdditionalBeanBuildItem.Builder()
                        .addBeanClasses(
                                InstanceInfoProvider.class,
                                ConsumerConnectionProvider.class,
                                ProducerConnectionProvider.class,
                                ConsumerChannelService.class,
                                ProducerChannelService.class,
                                AmqpConsumerContainer.class,
                                EventAppInfoProvider.class,
                                EventContext.class,
                                AmqpProducer.class,
                                ConnectionFactoryProvider.class,
                                MessageRequeueHandler.class,
                                MessageRequeueConsumer.class,
                                RetryQueues.class,
                                CorrelationIdProvider.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    EventAppInfoBuildItem scanForEventApp(final CombinedIndexBuildItem index, final ApplicationInfoBuildItem appInfoBuildItem) {
        final var eventAppScanner = new EventAppScanner(index.getIndex(), appInfoBuildItem.getName());
        return new EventAppInfoBuildItem(eventAppScanner.findEventAppContext().orElseThrow(() -> {
            throw new EventAppMissingException("EventApp annotation with basic info missing");
        }));
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void scanForMessageHandlers(CombinedIndexBuildItem index,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {
        MessageHandlerScanner scanner = new MessageHandlerScanner(index.getIndex());
        List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanner.scanMessageHandlerAnnotations();
        messageHandlerInfoBuildItems.forEach(messageHandlerProducer::produce);
    }

    @SuppressWarnings("unused")
    @BuildStep
    UnremovableBeanBuildItem unremovable() {
        // Any bean that has MyService in its set of bean types is considered unremovable
        return UnremovableBeanBuildItem.beanClassAnnotation("id.global.common.annotations.amqp");
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void configureConsumer(final BeanContainerBuildItem beanContainer, ConsumerInitRecorder consumerInitRecorder,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems) {
        if (!messageHandlerInfoBuildItems.isEmpty()) {
            consumerInitRecorder.initConsumers(beanContainer.getValue());
        }
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareMessageRequeueConsumer(final BeanContainerBuildItem beanContainer,
            MessageRequeueConsumerRecorder messageRequeueConsumerRecorder) {
        try {
            messageRequeueConsumerRecorder.registerMessageRequeueConsumer(beanContainer.getValue());
        } catch (IOException e) {
            LOG.error("Could not record retry consumer init", e);
        }

    }

    @SuppressWarnings("unused")
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
                Class<?> returnEventClass = col.getReturnType().kind() == Type.Kind.CLASS
                        ? cl.loadClass(col.getReturnType().asClassType().name().toString())
                        : null;

                MethodHandleContext methodHandleContext = new MethodHandleContext(handlerClass, eventClass,
                        returnEventClass, col.getMethodName());
                AmqpContext amqpContext = new AmqpContext(col.getName(),
                        col.getBindingKeys(),
                        col.getExchangeType(),
                        col.getScope(),
                        col.isDurable(),
                        col.isAutoDelete(),
                        col.isQueuePerInstance(),
                        col.getPrefetchCount(),
                        col.getTtl(),
                        col.getDeadLetterQueue());

                LOG.info("Registering handler. Handler class = " + handlerClass.getName() +
                        " eventClass = " + eventClass.getName() +
                        " methodName = " + col.getMethodName());
                if (returnEventClass != null) {
                    LOG.info("we have reply event: {}", returnEventClass);
                }

                methodHandleRecorder.registerConsumer(beanContainer.getValue(), methodHandleContext, amqpContext);

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IOException e) {
                LOG.error("Could not record method handle", e);
            }
        });
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void provideEventAppContext(final BeanContainerBuildItem beanContainer, EventAppInfoBuildItem eventAppInfoBuildItems,
            EventAppRecorder eventAppRecorder) {
        eventAppRecorder.registerEventAppContext(beanContainer.getValue(), eventAppInfoBuildItems.getEventAppContext());
    }

    @SuppressWarnings("unused")
    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("id.global.common", "globalid-common"));
    }
}
