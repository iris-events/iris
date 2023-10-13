package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.Message;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

public class RpcResponseClassParser {

    private static final String RPC_RESPONSE_CLASS_PARAM = "rpcResponse";

    public static Type getFromAnnotationClass(Message messageAnnotation) {
        return Type.create(DotName.createSimple(messageAnnotation.rpcResponse()), Type.Kind.CLASS);
    }

    public static Type getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        final var annotationValue = annotation.valueWithDefault(index, RPC_RESPONSE_CLASS_PARAM);
        if (annotationValue == null
                || annotationValue.asClass().equals(Type.create(DotName.createSimple(java.lang.Void.class), Type.Kind.CLASS))) {
            return null;
        }
        return annotationValue.asClass();
    }
}
