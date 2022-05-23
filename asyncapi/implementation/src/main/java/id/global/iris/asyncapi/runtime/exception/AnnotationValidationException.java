package id.global.iris.asyncapi.runtime.exception;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;

public class AnnotationValidationException extends RuntimeException {
    private final List<AnnotationInstance> faultyAnnotations;

    public AnnotationValidationException(String msg, List<AnnotationInstance> faultyAnnotations) {
        super(msg);
        this.faultyAnnotations = faultyAnnotations;
    }

    public List<AnnotationInstance> getFaultyAnnotations() {
        return faultyAnnotations;
    }
}
