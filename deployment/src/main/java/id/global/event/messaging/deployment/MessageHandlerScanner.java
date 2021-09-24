package id.global.event.messaging.deployment;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;

public class MessageHandlerScanner {
    private static final Logger LOG = Logger.getLogger(MessageHandlerScanner.class);

    public static final String QUEUE_PARAM = "queue";
    public static final String EXCHANGE_PARAM = "exchange";
    public static final String BINDING_KEYS_PARAM = "bindingKeys";

    private final IndexView index;

    public MessageHandlerScanner(IndexView index) {
        this.index = index;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {
        DotName messageHandlerDotName = DotName.createSimple(MessageHandler.class.getCanonicalName());
        DotName fanoutMessageHandlerDotName = DotName.createSimple(FanoutMessageHandler.class.getCanonicalName());
        DotName topicMessageHandlerDotName = DotName.createSimple(TopicMessageHandler.class.getCanonicalName());

        Stream<AnnotationInstance> directAnnotations = index.getAnnotations(messageHandlerDotName).stream();
        Stream<AnnotationInstance> fanoutAnnotations = index.getAnnotations(fanoutMessageHandlerDotName).stream();
        Stream<AnnotationInstance> topicAnnotations = index.getAnnotations(topicMessageHandlerDotName).stream();

        return Stream
                .concat(Stream.concat(scanDirectMessageHandlerAnnotations(directAnnotations),
                        scanFanoutMessageHandlerAnnotations(fanoutAnnotations)),
                        scanTopicMessageHandlerAnnotations(topicAnnotations))
                .collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanTopicMessageHandlerAnnotations(
            Stream<AnnotationInstance> topicAnnotations) {
        return topicAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {

            validateTopic(annotationInstance);

            MethodInfo methodInfo = annotationInstance.target().asMethod();
            String exchange = annotationInstance.value(EXCHANGE_PARAM).asString();
            String[] bindingKeys = annotationInstance.value(BINDING_KEYS_PARAM).asStringArray();
            // Implement parsing other parameters if needed here

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    null,
                    exchange,
                    bindingKeys,
                    TOPIC);
        });
    }

    private Stream<MessageHandlerInfoBuildItem> scanFanoutMessageHandlerAnnotations(
            Stream<AnnotationInstance> fanoutAnnotations) {
        return fanoutAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {

            validateFanout(annotationInstance);

            MethodInfo methodInfo = annotationInstance.target().asMethod();
            String exchange = annotationInstance.value(EXCHANGE_PARAM).asString();
            // Implement parsing other parameters if needed here

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    null,
                    exchange,
                    null,
                    FANOUT);
        });
    }

    private Stream<MessageHandlerInfoBuildItem> scanDirectMessageHandlerAnnotations(
            Stream<AnnotationInstance> directAnnotations) {
        return directAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {
            validateDirect(annotationInstance);

            String exchange = annotationInstance.value(EXCHANGE_PARAM) != null
                    ? annotationInstance.value(EXCHANGE_PARAM).asString()
                    : null;
            String queue = annotationInstance.value(QUEUE_PARAM).asString();
            // Implement parsing other parameters if needed here

            MethodInfo methodInfo = annotationInstance.target().asMethod();
            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    queue,
                    exchange,
                    null,
                    DIRECT);
        });
    }

    private void validateDirect(AnnotationInstance annotationInstance) {
        validateMethodParamNumber(annotationInstance);
        validateMethodParam(annotationInstance);
        validateParam(annotationInstance, QUEUE_PARAM);
    }

    private void validateFanout(AnnotationInstance annotationInstance) {
        validateMethodParamNumber(annotationInstance);
        validateMethodParam(annotationInstance);
        validateParam(annotationInstance, EXCHANGE_PARAM);
    }

    private void validateTopic(AnnotationInstance annotationInstance) {
        validateMethodParamNumber(annotationInstance);
        validateMethodParam(annotationInstance);
        validateParam(annotationInstance, EXCHANGE_PARAM);
        validateParam(annotationInstance, BINDING_KEYS_PARAM);
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private void validateMethodParam(AnnotationInstance annotationInstance) {
        MethodInfo methodInfo = annotationInstance.target().asMethod();
        Type parameterType = methodInfo.parameters().get(0);
        ClassInfo classByName = index.getClassByName(parameterType.name());

        if (classByName == null) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s can not have external dependency classes as parameters",
                            methodInfo.declaringClass(),
                            methodInfo.name()));
        }

    }

    private void validateMethodParamNumber(AnnotationInstance annotationInstance) {
        if (annotationInstance.target().asMethod().parameters().isEmpty()
                || annotationInstance.target().asMethod().parameters().size() > 1) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s must declare exactly one parameter that represents the event",
                            annotationInstance.target().asMethod().declaringClass(),
                            annotationInstance.target().asMethod().name()));
        }
    }

    private void validateParam(AnnotationInstance annotationInstance, String param) {
        if (annotationInstance.value(param) == null) {
            throw new MessageHandlerValidationException(
                    String.format("Parameter \"%s\" missing on MessageHandler annotation on %s::%s",
                            param,
                            annotationInstance.target().asMethod().declaringClass(),
                            annotationInstance.target().asMethod().name()));
        }
    }
}
