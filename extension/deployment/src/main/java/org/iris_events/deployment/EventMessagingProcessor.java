package org.iris_events.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iris_events.annotations.Scope;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.iris_events.deployment.scanner.Scanner;
import org.iris_events.runtime.BasicPropertiesProvider;
import org.iris_events.runtime.EventAppInfoProvider;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.TimestampProvider;
import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.runtime.channel.ConsumerChannelService;
import org.iris_events.runtime.channel.ProducerChannelService;
import org.iris_events.runtime.configuration.IrisBuildConfiguration;
import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.runtime.connection.ConnectionFactoryProvider;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;
import org.iris_events.runtime.connection.ProducerConnectionProvider;
import org.iris_events.consumer.ConsumerContainer;
import org.iris_events.consumer.FrontendEventConsumer;
import org.iris_events.context.EventAppContext;
import org.iris_events.context.EventContext;
import org.iris_events.context.IrisContext;
import org.iris_events.context.MethodHandleContext;
import org.iris_events.exception.IrisExceptionHandler;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.EventProducer;
import org.iris_events.producer.ProducedEventExchangeInitializer;
import org.iris_events.runtime.recorder.ConsumerInitRecorder;
import org.iris_events.runtime.recorder.EventAppRecorder;
import org.iris_events.runtime.recorder.MethodHandleRecorder;
import org.iris_events.runtime.recorder.ProducerDefinedExchangesRecorder;
import org.iris_events.runtime.requeue.MessageRequeueHandler;
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

        IrisBuildConfiguration config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    private static final String FEATURE = "iris";
    private static final Logger log = LoggerFactory.getLogger(EventMessagingProcessor.class);

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareIrisBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer.produce(
                new AdditionalBeanBuildItem.Builder()
                        .addBeanClasses(
                                InstanceInfoProvider.class,
                                ConsumerConnectionProvider.class,
                                ProducerConnectionProvider.class,
                                ConsumerChannelService.class,
                                ProducerChannelService.class,
                                ConsumerContainer.class,
                                EventAppInfoProvider.class,
                                EventContext.class,
                                EventProducer.class,
                                ConnectionFactoryProvider.class,
                                MessageRequeueHandler.class,
                                CorrelationIdProvider.class,
                                IrisJwtValidator.class,
                                FrontendEventConsumer.class,
                                IrisExceptionHandler.class,
                                QueueNameProvider.class,
                                IrisReadinessCheck.class,
                                IrisLivenessCheck.class,
                                TimestampProvider.class,
                                BasicPropertiesProvider.class,
                                IrisRabbitMQConfig.class,
                                ProducedEventExchangeInitializer.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void scanForMessageHandlers(CombinedIndexBuildItem combinedIndexBuildItem, ApplicationInfoBuildItem appInfo,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {

        final var index = combinedIndexBuildItem.getIndex();
        final var scanner = new Scanner(index, appInfo.getName());
        scanner.scanEventHandlerAnnotations()
                .forEach(messageHandlerProducer::produce);
    }

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void scanForMessages(CombinedIndexBuildItem combinedIndexBuildItem, ApplicationInfoBuildItem appInfo,
            BuildProducer<MessageInfoBuildItem> messageInfoProducer) {
        final var scanner = new Scanner(combinedIndexBuildItem.getIndex(), appInfo.getName());
        final var irisGeneratedMessageAnnotations = scanner.scanIrisGeneratedAnnotations();

        scanner.scanMessageAnnotations()
                .stream().filter(messageInfoBuildItem -> !irisGeneratedMessageAnnotations.stream()
                        .map(MessageInfoBuildItem::getAnnotatedClassInfo).toList()
                        .contains(messageInfoBuildItem.getAnnotatedClassInfo()))
                .forEach(messageInfoProducer::produce);
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void initProducerDefinedExchangeRequests(BeanContainerBuildItem beanContainer,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems,
            List<MessageInfoBuildItem> messageInfoBuildItems,
            ProducerDefinedExchangesRecorder producerDefinedExchangesRecorder) {

        final var handledMessageTypeNames = messageHandlerInfoBuildItems.stream()
                .map(messageHandlerInfoBuildItem -> messageHandlerInfoBuildItem.getParameterType().name())
                .toList();
        messageInfoBuildItems.stream()
                .filter(buildItem -> !handledMessageTypeNames.contains(buildItem.getAnnotatedClassInfo().name()))
                .forEach(buildItem -> producerDefinedExchangesRecorder.registerProducerDefinedExchange(
                        beanContainer.getValue(),
                        buildItem.getName(),
                        buildItem.getExchangeType(),
                        buildItem.getScope()
                ));
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = EventMessagingEnabled.class)
    void declareProducerDefinedExchanges(final BeanContainerBuildItem beanContainer,
            ProducerDefinedExchangesRecorder producerDefinedExchangesRecorder) {
        producerDefinedExchangesRecorder.init(beanContainer.getValue());

    }

    @SuppressWarnings("unused")
    @BuildStep
    UnremovableBeanBuildItem unremovable() {
        // Any bean that contains or is annotated with annotation defined within the given package is considered unremovable
        return UnremovableBeanBuildItem.beanClassAnnotation("org.iris-events.common.annotations");
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
        List<String> handlers = new ArrayList<>();
        messageHandlerInfoBuildItems.forEach(col -> {
            try {
                Class<?> handlerClass = cl.loadClass(col.getDeclaringClass().name().toString());
                Class<?> eventClass = cl.loadClass(col.getParameterType().asClassType().name().toString());
                Class<?> returnEventClass = col.getReturnType().kind() == Type.Kind.CLASS
                        ? cl.loadClass(col.getReturnType().asClassType().name().toString())
                        : null;

                MethodHandleContext methodHandleContext = new MethodHandleContext(handlerClass, eventClass,
                        returnEventClass, col.getMethodName());
                IrisContext irisContext = new IrisContext(col.getName(),
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

                if (col.getScope() != Scope.FRONTEND) {
                    methodHandleRecorder.registerConsumer(beanContainer.getValue(), methodHandleContext, irisContext);
                } else {
                    methodHandleRecorder.registerFrontendCallback(beanContainer.getValue(), methodHandleContext, irisContext);
                }

                StringBuilder sb = new StringBuilder();
                sb.append(handlerClass.getSimpleName()).append("#").append(col.getMethodName());
                sb.append(" (").append(eventClass.getSimpleName());
                if (returnEventClass != null) {
                    sb.append(" -> ").append(returnEventClass.getSimpleName());
                }
                sb.append(")");

                handlers.add(sb.toString());

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IOException e) {
                log.error("Could not record method handle. methodName: " + col.getMethodName(), e);
            }
        });
        log.info("Registered method handlers: " + String.join(", ", handlers));
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
        indexDependency.produce(new IndexDependencyBuildItem("jakarta.annotation", "jakarta.annotation-api"));
        indexDependency.produce(new IndexDependencyBuildItem("org.iris-events", "quarkus-iris"));
    }

    @SuppressWarnings("unused")
    @BuildStep
    HealthBuildItem addReadinessCheck(Capabilities capabilities, IrisBuildConfiguration configuration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem(IrisReadinessCheck.class.getName(),
                    configuration.readinessCheckEnabled);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    @BuildStep
    HealthBuildItem addLivenessCheck(Capabilities capabilities, IrisBuildConfiguration configuration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem(IrisLivenessCheck.class.getName(),
                    configuration.livenessCheckEnabled);
        } else {
            return null;
        }
    }
}
