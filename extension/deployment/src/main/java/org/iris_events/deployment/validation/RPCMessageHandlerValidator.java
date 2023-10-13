package org.iris_events.deployment.validation;

import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.iris_events.deployment.constants.AnnotationInstanceParams;
import org.iris_events.deployment.scanner.ScannerUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class RPCMessageHandlerValidator implements AnnotationInstanceValidator {
    private final IndexView index;

    public RPCMessageHandlerValidator(final IndexView indexView) {
        this.index = indexView;
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var methodInfo = annotationInstance.target().asMethod();
        final var returnType = methodInfo.returnType();

        if (isRpc(annotationInstance)) {
            if (returnType.kind() == Type.Kind.VOID) {
                throw new MessageHandlerValidationException(
                        String.format(
                                "MessageHandler annotated method %s::%s is defined as a RPC message handler and must have a valid return type.",
                                methodInfo.declaringClass(), methodInfo.name()));
            }
            if (!getMessageScope(annotationInstance).equals(Scope.INTERNAL)) {
                throw new MessageHandlerValidationException(
                        String.format(
                                "MessageHandler annotated method %s::%s is defined as a RPC message handler and must have use INTERNAL scope.",
                                methodInfo.declaringClass(), methodInfo.name()));

            }
        }
    }

    private static boolean isRpc(final AnnotationInstance annotationInstance) {
        return annotationInstance.value(AnnotationInstanceParams.RPC_RESPONSE_PARAM) != null
                && !annotationInstance.value(AnnotationInstanceParams.RPC_RESPONSE_PARAM).asClass().name().toString()
                        .equals(Void.class.getName());
    }

    private Scope getMessageScope(final AnnotationInstance annotationInstance) {
        final var methodInfo = getMethodInfo(annotationInstance);
        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, this.index);

        return MessageScopeParser.getFromAnnotationInstance(messageAnnotation, this.index);
    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }
}
