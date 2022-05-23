package id.global.iris.messaging.deployment;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.common.iris.annotations.Scope;
import id.global.iris.messaging.deployment.scanner.MessageHandlerScanner;
import id.global.iris.messaging.runtime.AmqpBasicPropertiesProvider;
import id.global.iris.messaging.runtime.EventAppInfoProvider;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.QueueNameProvider;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.auth.GidJwtValidator;
import id.global.iris.messaging.runtime.channel.ConsumerChannelService;
import id.global.iris.messaging.runtime.channel.ProducerChannelService;
import id.global.iris.messaging.runtime.configuration.AmqpBuildConfiguration;
import id.global.iris.messaging.runtime.connection.ConnectionFactoryProvider;
import id.global.iris.messaging.runtime.connection.ConsumerConnectionProvider;
import id.global.iris.messaging.runtime.connection.ProducerConnectionProvider;
import id.global.iris.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.iris.messaging.runtime.consumer.FrontendAmqpConsumer;
import id.global.iris.messaging.runtime.context.AmqpContext;
import id.global.iris.messaging.runtime.context.EventAppContext;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.context.MethodHandleContext;
import id.global.iris.messaging.runtime.exception.AmqpExceptionHandler;
import id.global.iris.messaging.runtime.health.IrisHealthCheck;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import id.global.iris.messaging.runtime.producer.CorrelationIdProvider;
import id.global.iris.messaging.runtime.recorder.ConsumerInitRecorder;
import id.global.iris.messaging.runtime.recorder.EventAppRecorder;
import id.global.iris.messaging.runtime.recorder.MethodHandleRecorder;
import id.global.iris.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class EventMessagingProcessor {

    public static class EventMessagingEnabled implements BooleanSupplier {

        AmqpBuildConfiguration config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    private static final String FEATURE = "quarkus-iris";
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
                                CorrelationIdProvider.class,
                                GidJwtValidator.class,
                                FrontendAmqpConsumer.class,
                                AmqpExceptionHandler.class,
                                QueueNameProvider.class,
                                IrisHealthCheck.class,
                                TimestampProvider.class,
                                AmqpBasicPropertiesProvider.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void scanForMessageHandlers(CombinedIndexBuildItem index, ApplicationInfoBuildItem appInfo,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {
        MessageHandlerScanner scanner = new MessageHandlerScanner(index.getIndex(), appInfo.getName());
        List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanner.scanMessageHandlerAnnotations();
        messageHandlerInfoBuildItems.forEach(messageHandlerProducer::produce);
    }

    @SuppressWarnings("unused")
    @BuildStep
    UnremovableBeanBuildItem unremovable() {
        // Any bean that contains or is annotated with annotation defined within the given package is considered unremovable
        return UnremovableBeanBuildItem.beanClassAnnotation("id.global.common.iris.annotations");
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
                        col.getDeadLetterQueue(),
                        col.getRolesAllowed());

                LOG.info("Registering handler. Handler class = " + handlerClass.getName() +
                        " eventClass = " + eventClass.getName() +
                        " methodName = " + col.getMethodName());
                if (returnEventClass != null) {
                    LOG.info("we have reply event: {}", returnEventClass);
                }

                if (col.getScope() != Scope.FRONTEND) {
                    methodHandleRecorder.registerConsumer(beanContainer.getValue(), methodHandleContext, amqpContext);
                } else {
                    methodHandleRecorder.registerFrontendCallback(beanContainer.getValue(), methodHandleContext, amqpContext);
                }

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IOException e) {
                LOG.error("Could not record method handle", e);
            }
        });
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void provideEventAppContext(final BeanContainerBuildItem beanContainer, ApplicationInfoBuildItem applicationInfoBuildItem,
            EventAppRecorder eventAppRecorder) {
        eventAppRecorder.registerEventAppContext(beanContainer.getValue(), new EventAppContext(
                applicationInfoBuildItem.getName()));
    }

    @SuppressWarnings("unused")
    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("id.global.common", "globalid-common"));
        indexDependency.produce(new IndexDependencyBuildItem("id.global.iris", "quarkus-iris"));
    }

    @SuppressWarnings("unused")
    @BuildStep
    HealthBuildItem addHealthCheck(Capabilities capabilities, AmqpBuildConfiguration configuration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem("id.global.iris.messaging.runtime.health.IrisHealthCheck",
                    configuration.healthCheckEnabled);
        } else {
            return null;
        }
    }
}
