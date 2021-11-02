package io.smallrye.asyncapi.runtime.scanner.model;

import java.util.List;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonSchemaInfo {
    private final DotName annotationName;
    private String eventSimpleName;
    private final ObjectNode generatedSchema;
    private final List<AnnotationValue> annotationValues;
    private final boolean isGeneratedClass;

    public JsonSchemaInfo(DotName annotationName, String eventSimpleName, ObjectNode generatedSchema,
            List<AnnotationValue> annotationValues, boolean isGeneratedClass) {

        this.annotationName = annotationName;
        this.eventSimpleName = eventSimpleName;
        this.generatedSchema = generatedSchema;
        this.annotationValues = annotationValues;
        this.isGeneratedClass = isGeneratedClass;
    }

    public DotName getAnnotationName() {
        return annotationName;
    }

    public String getEventSimpleName() {
        return eventSimpleName;
    }

    public ObjectNode getGeneratedSchema() {
        return generatedSchema;
    }

    public List<AnnotationValue> getAnnotationValues() {
        return annotationValues;
    }

    public boolean isGeneratedClass() {
        return isGeneratedClass;
    }
}
