package io.smallrye.asyncapi.runtime.scanner.model;

public class AaiSchemaAdditionalProperties {
    private final boolean isGeneratedClass;

    public AaiSchemaAdditionalProperties(boolean isGeneratedClass) {
        this.isGeneratedClass = isGeneratedClass;
    }

    public boolean isGeneratedClass() {
        return isGeneratedClass;
    }
}
