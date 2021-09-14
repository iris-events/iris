package id.global.event.messaging.deployment;

import java.io.IOException;
import java.util.List;

import org.jboss.logging.Logger;

import id.global.event.messaging.runtime.ConsumerConfigRecorder;
import id.global.event.messaging.runtime.MethodHandleRecorder;
import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class EventMessagingProcessor {

    private static final String FEATURE = "event-messaging";
    private static final Logger LOG = Logger.getLogger(EventMessagingProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void declareAmqpBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer.produce(
                new AdditionalBeanBuildItem.Builder()
                        .addBeanClasses(
                                AmqpConsumerContainer.class,
                                EventContext.class,
                                AmqpProducer.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build());
    }

    @BuildStep
    void scanForMessageHandlers(CombinedIndexBuildItem index,
            BuildProducer<MessageHandlerInfoBuildItem> messageHandlerProducer) {
        MessageHandlerScanner scanner = new MessageHandlerScanner(index.getIndex());
        List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems = scanner.scanMessageHandlerAnnotations();
        messageHandlerInfoBuildItems.forEach(messageHandlerProducer::produce);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureConsumer(final BeanContainerBuildItem beanContainer, ConsumerConfigRecorder consumerConfigRecorder) {
        // init the consumer with config properties
        // this should be moved to its own build step
        consumerConfigRecorder.initConsumerConfig(beanContainer.getValue());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
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
