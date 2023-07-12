package org.iris_events.asyncapi.runtime.scanner.model;

import java.util.List;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonSchemaInfo {
    private final DotName annotationName;
    private final String eventSimpleName;
    private final ObjectNode generatedSchema;
    private final List<AnnotationValue> annotationValues;
    private final boolean isGeneratedClass;
    private final Integer cacheTtl;

    public JsonSchemaInfo(DotName annotationName, String eventSimpleName, ObjectNode generatedSchema,
            List<AnnotationValue> annotationValues, boolean isGeneratedClass, final Integer cacheTtl) {

        this.annotationName = annotationName;
        this.eventSimpleName = eventSimpleName;
        this.generatedSchema = generatedSchema;
        this.annotationValues = annotationValues;
        this.isGeneratedClass = isGeneratedClass;
        this.cacheTtl = cacheTtl;
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

    public Integer getCacheTtl() {
        return cacheTtl;
    }
}
