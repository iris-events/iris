package org.iris_events.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.iris_events.annotations.Scope;
import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.consumer.ConsumerContainer;
import org.iris_events.consumer.FrontendEventConsumer;
import org.iris_events.context.EventAppContext;
import org.iris_events.context.EventContext;
import org.iris_events.context.IrisContext;
import org.iris_events.context.MethodHandleContext;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.iris_events.deployment.builditem.RpcMappingBuildItem;
import org.iris_events.deployment.scanner.Scanner;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.EventProducer;
import org.iris_events.producer.ProducedEventExchangeInitializer;
import org.iris_events.runtime.BasicPropertiesProvider;
import org.iris_events.runtime.EventAppInfoProvider;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.IrisExceptionHandler;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.TimestampProvider;
import org.iris_events.runtime.channel.ConsumerChannelService;
import org.iris_events.runtime.channel.ProducerChannelService;
import org.iris_events.runtime.configuration.IrisBuildTimeConfig;
import org.iris_events.runtime.connection.ConnectionFactoryProvider;
import org.iris_events.runtime.connection.ConsumerConnectionProvider;
import org.iris_events.runtime.connection.ProducerConnectionProvider;
import org.iris_events.runtime.recorder.ConsumerInitRecorder;
import org.iris_events.runtime.recorder.EventAppRecorder;
import org.iris_events.runtime.recorder.MethodHandleRecorder;
import org.iris_events.runtime.recorder.ProducerDefinedExchangesRecorder;
import org.iris_events.runtime.recorder.RpcMappingRecorder;
import org.iris_events.runtime.requeue.MessageRequeueHandler;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

@BuildSteps(onlyIf = IrisEnabled.class)
class IrisProcessor {

    private static final String FEATURE = "iris";
    private static final Logger log = LoggerFactory.getLogger(IrisProcessor.class);

    @SuppressWarnings("unused")
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @SuppressWarnings("unused")
    @BuildStep
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
                                FrontendEventConsumer.class,
                                IrisExceptionHandler.class,
                                QueueNameProvider.class,
                                IrisReadinessCheck.class,
                                IrisLivenessCheck.class,
                                TimestampProvider.class,
                                BasicPropertiesProvider.class,
                                ProducedEventExchangeInitializer.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }



    @SuppressWarnings("unused")
    @BuildStep
    void scanForMessageHandlers(CombinedIndexBuildItem combinedIndexBuildItem, ApplicationInfoBuildItem appInfo,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {

        final var index = combinedIndexBuildItem.getIndex();
        final var scanner = new Scanner(index, appInfo.getName());
        scanner.scanEventHandlerAnnotations()
                .forEach(messageHandlerProducer::produce);
    }

    @SuppressWarnings("unused")
    @BuildStep
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
    @BuildStep
    void scanForRpcMessages(CombinedIndexBuildItem combinedIndexBuildItem, ApplicationInfoBuildItem appInfo,
            List<MessageInfoBuildItem> messageInfoBuildItems,
            BuildProducer<RpcMappingBuildItem> rpcMappingBuildItemBuildProducer) {
        final var scanner = new Scanner(combinedIndexBuildItem.getIndex(), appInfo.getName());
        final var generatedMessageAnnotations = scanner.scanIrisGeneratedAnnotations();

        final var rpcAnnotationCandidates = new ArrayList<MessageInfoBuildItem>();
        rpcAnnotationCandidates.addAll(messageInfoBuildItems);
        rpcAnnotationCandidates.addAll(generatedMessageAnnotations);

        final var rpcReplyToMapping = rpcAnnotationCandidates.stream()
                .filter(messageInfoBuildItem -> messageInfoBuildItem.getRpcResponseType() != null
                        && !messageInfoBuildItem.getRpcResponseType()
                                .equals(Type.create(DotName.createSimple(java.lang.Void.class), Type.Kind.CLASS)))
                .collect(Collectors.toMap(
                        messageInfoBuildItem -> messageInfoBuildItem.getRpcResponseType().name().toString(),
                        messageInfoBuildItem -> {
                            final var matchingMessageInfoBuildItem = rpcAnnotationCandidates.stream()
                                    .filter(matchMessageInfoBuildItem -> matchMessageInfoBuildItem.getAnnotatedClassInfo()
                                            .name()
                                            .equals(messageInfoBuildItem.getRpcResponseType().name()))
                                    .findFirst();
                            if (matchingMessageInfoBuildItem.isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Rpc request message should always have its response counterpart defined. Message class: "
                                                + messageInfoBuildItem.getAnnotatedClassInfo().name().toString()
                                                + " messageInfoBuildItem rpcResponseType: "
                                                + messageInfoBuildItem.getRpcResponseType());
                            }
                            return matchingMessageInfoBuildItem.get().getName();
                        }));
        rpcMappingBuildItemBuildProducer.produce(new RpcMappingBuildItem(rpcReplyToMapping));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void initRpcReplyToMappingBean(BeanContainerBuildItem beanContainer, List<RpcMappingBuildItem> rpcMappingBuildItems,
            RpcMappingRecorder rpcMappingRecorder) {

        rpcMappingBuildItems.stream().forEach(rpcMappingBuildItem -> rpcMappingRecorder
                .registerRpcMappings(beanContainer.getValue(), rpcMappingBuildItem.getRpcReplyToMapping()));

    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
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
                        buildItem.getScope()));
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void declareProducerDefinedExchanges(final BeanContainerBuildItem beanContainer,
            ProducerDefinedExchangesRecorder producerDefinedExchangesRecorder) {
        producerDefinedExchangesRecorder.init(beanContainer.getValue());

    }

    @SuppressWarnings("unused")
    @BuildStep
    UnremovableBeanBuildItem unremovable() {
        // Any bean that contains or is annotated with annotation defined within the given package is considered unremovable
        return UnremovableBeanBuildItem.beanClassAnnotation("org.iris_events.annotations");
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureConsumer(final BeanContainerBuildItem beanContainer, ConsumerInitRecorder consumerInitRecorder,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems) {
        if (!messageHandlerInfoBuildItems.isEmpty()) {
            consumerInitRecorder.initConsumers(beanContainer.getValue());
        }
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void declareMessageHandlers(final BeanContainerBuildItem beanContainer,
            List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems,
            List<MessageInfoBuildItem> messageInfoBuildItems,
            MethodHandleRecorder methodHandleRecorder) {
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        List<String> handlers = new ArrayList<>();
        messageHandlerInfoBuildItems.forEach(mhBuildItem -> {
            try {
                final var handlerClass = cl.loadClass(mhBuildItem.getDeclaringClass().name().toString());
                final var eventClass = cl.loadClass(mhBuildItem.getParameterType().asClassType().name().toString());
                final var returnEventClass = getMessageHandlerReturnEventClass(cl, mhBuildItem);

                final var rpcReturnEventName = getMessageRpcReturnName(mhBuildItem, messageInfoBuildItems);
                if (log.isInfoEnabled()) {
                    log.trace("Message handler build item for " + mhBuildItem.getName() + " eventClass: " + eventClass
                            + " returnEventClass: " + returnEventClass + " rpcReturnEventClass: " + rpcReturnEventName
                            + " bindingKeys: " + mhBuildItem.getBindingKeys());
                }

                final var methodHandleContext = new MethodHandleContext(handlerClass, eventClass,
                        returnEventClass, mhBuildItem.getMethodName());
                final var irisContext = new IrisContext(mhBuildItem.getName(),
                        mhBuildItem.getBindingKeys(),
                        mhBuildItem.getExchangeType(),
                        mhBuildItem.getScope(),
                        mhBuildItem.isDurable(),
                        mhBuildItem.isAutoDelete(),
                        mhBuildItem.isQueuePerInstance(),
                        mhBuildItem.getPrefetchCount(),
                        mhBuildItem.getTtl(),
                        mhBuildItem.getDeadLetterQueue(),
                        mhBuildItem.getRolesAllowed(),
                        rpcReturnEventName);

                if (mhBuildItem.getScope() != Scope.FRONTEND) {
                    methodHandleRecorder.registerConsumer(beanContainer.getValue(), methodHandleContext, irisContext);
                } else {
                    methodHandleRecorder.registerFrontendCallback(beanContainer.getValue(), methodHandleContext, irisContext);
                }

                final var sb = new StringBuilder();
                sb.append(handlerClass.getSimpleName())
                        .append("#").append(mhBuildItem.getMethodName())
                        .append(" (").append(eventClass.getSimpleName());
                if (returnEventClass != null) {
                    sb.append(" -> ").append(returnEventClass.getSimpleName());
                }
                sb.append(")");

                handlers.add(sb.toString());

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IOException e) {
                log.error("Could not record method handle. methodName: " + mhBuildItem.getMethodName(), e);
            }
        });
        log.info("Registered method handlers: " + String.join(", ", handlers));
    }

    private String getMessageRpcReturnName(final MessageHandlerInfoBuildItem mhBuildItem,
            final List<MessageInfoBuildItem> messageInfoBuildItems) {
        // handler build item
        final var messageHandlerParameterType = mhBuildItem.getParameterType();
        // related messageInfo build item
        final var messageHandlerParameterMessageInfoBuildItem = messageInfoBuildItems.stream()
                .filter(messageInfoBuildItem -> messageInfoBuildItem.getAnnotatedClassInfo().name()
                        .equals(messageHandlerParameterType.name()))
                .findFirst().orElse(null);
        if (messageHandlerParameterMessageInfoBuildItem == null) {
            return null;
        }

        // RPC type of that messageInfo, might be null!
        final var messageRpcType = messageHandlerParameterMessageInfoBuildItem.getRpcResponseType();
        if (messageRpcType == null) {
            return null;
        }
        // message has RPC type defined, find that event name
        return messageInfoBuildItems.stream()
                .filter(messageInfoBuildItem -> messageInfoBuildItem.getAnnotatedClassInfo().name()
                        .equals(messageRpcType.name()))
                .findFirst()
                .map(MessageInfoBuildItem::getName).orElse(null);
    }

    @SuppressWarnings("unused")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
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


    @Nullable
    private static Class<?> getMessageHandlerReturnEventClass(final QuarkusClassLoader cl,
            final MessageHandlerInfoBuildItem col) throws ClassNotFoundException {
        return col.getReturnType().kind() == Type.Kind.CLASS
                ? cl.loadClass(col.getReturnType().asClassType().name().toString())
                : null;
    }
}
