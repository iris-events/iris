package org.iris_events.asyncapi.runtime.scanner.validator;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.iris_events.asyncapi.runtime.exception.AnnotationValidationException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import org.iris_events.annotations.Message;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.RoutingKeyParser;

public class MessageAnnotationValidator {

    // TODO: opensource - make it configurable
    private final List<String> SERVICES_TO_IGNORE_RESERVED = List.of("org.iris_events.events.iris-subscription",
            "org.iris_events.events.iris-manager");

    public void validateReservedNames(List<AnnotationInstance> messageAnnotations, String projectName, String projectGroupId) {
        validateIsMessageAnnotations(messageAnnotations);

        List<String> reservedExchangeNames = ReservedIrisNamesProvider.getReservedNames();

        if (SERVICES_TO_IGNORE_RESERVED.contains(String.join(".", List.of(projectGroupId, projectName)))) {
            return;
        }
        List<AnnotationInstance> annotationsWithReserved = messageAnnotations.stream()
                .filter(containsReservedNames(reservedExchangeNames))
                .collect(Collectors.toList());

        if (!annotationsWithReserved.isEmpty()) {
            throw new AnnotationValidationException(String.format("Annotations use reserved names. Annotations: %s",
                    stringifyAnnotations(annotationsWithReserved)), annotationsWithReserved);
        }
    }

    private Predicate<AnnotationInstance> containsReservedNames(List<String> reservedExchangeNames) {
        return messageAnnotation -> {
            String exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
            String routingKey = RoutingKeyParser.getFromAnnotationInstance(messageAnnotation);
            return reservedExchangeNames.contains(exchange) || reservedExchangeNames.contains(routingKey);
        };
    }

    private void validateIsMessageAnnotations(List<AnnotationInstance> messageAnnotations) {
        validateIsClassAnnotation(messageAnnotations);

        List<AnnotationInstance> nonMessageAnnotations = messageAnnotations.stream()
                .filter(messageAnnotation -> !messageAnnotation.name().equals(DotName.createSimple(Message.class.getName())))
                .collect(Collectors.toList());

        if (!nonMessageAnnotations.isEmpty()) {
            throw new AnnotationValidationException(
                    String.format("Annotation not of Message class. Annotations: %s",
                            stringifyAnnotations(nonMessageAnnotations)),
                    nonMessageAnnotations);
        }
    }

    private void validateIsClassAnnotation(List<AnnotationInstance> messageAnnotations) {
        List<AnnotationInstance> nonClassAnnotations = messageAnnotations.stream()
                .filter(annotationInstance -> !annotationInstance.target().kind().equals(AnnotationTarget.Kind.CLASS) &&
                        !annotationInstance.target().kind().equals(AnnotationTarget.Kind.RECORD_COMPONENT))
                .collect(Collectors.toList());

        if (!nonClassAnnotations.isEmpty()) {
            throw new AnnotationValidationException(
                    String.format("Annotation target is not class. Annotations: %s", stringifyAnnotations(nonClassAnnotations)),
                    nonClassAnnotations);
        }
    }

    private String stringifyAnnotations(List<AnnotationInstance> nonClassAnnotations) {
        StringBuffer sb = new StringBuffer();
        nonClassAnnotations.forEach(annotationInstance -> sb
                .append(String.format("%s@%s", annotationInstance.name(),
                        stringifyAnnotationTarget(annotationInstance.target()))));
        return sb.toString();
    }

    private String stringifyAnnotationTarget(AnnotationTarget target) {
        switch (target.kind()) {
            case CLASS -> {
                return String.format("%s:%s", target.asClass().name(), AnnotationTarget.Kind.CLASS);
            }
            case METHOD -> {
                return String.format("%s:%s", target.asMethod().name(), AnnotationTarget.Kind.METHOD);
            }
            case METHOD_PARAMETER -> {
                return String.format("%s:%s", target.asMethodParameter().name(), AnnotationTarget.Kind.METHOD_PARAMETER);
            }
            case RECORD_COMPONENT -> {
                return String.format("%s:%s", target.asRecordComponent().name(), AnnotationTarget.Kind.RECORD_COMPONENT);
            }
            case TYPE -> {
                return String.format("%s", AnnotationTarget.Kind.TYPE);
            }
            case FIELD -> {
                return String.format("%s:%s", target.asField().name(), AnnotationTarget.Kind.FIELD);
            }
            default -> {
                return "Unknown annotation target kind";
            }
        }
    }
}
