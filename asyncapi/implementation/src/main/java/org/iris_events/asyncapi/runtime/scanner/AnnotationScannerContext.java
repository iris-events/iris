package org.iris_events.asyncapi.runtime.scanner;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.iris_events.asyncapi.runtime.scanner.model.GidAsyncApi26Schema;
import org.jboss.jandex.AnnotationInstance;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;

public class AnnotationScannerContext {
    private final FilteredIndexView index;
    private final AsyncApi26Document asyncApi;
    private final Map<String, GidAsyncApi26Schema> definitionSchemaMap;
    private final Collection<AnnotationInstance> generatedClassAnnotations;

    public AnnotationScannerContext(FilteredIndexView index, AsyncApi26Document asyncApi,
            Collection<AnnotationInstance> generatedClassAnnotations) {
        this.index = index;
        this.asyncApi = asyncApi;
        this.generatedClassAnnotations = generatedClassAnnotations;

        this.definitionSchemaMap = new LinkedHashMap<>();
    }

    public AsyncApi26Document getAsyncApi() {
        return this.asyncApi;
    }

    public FilteredIndexView getIndex() {
        return index;
    }

    public void addDefinitionSchema(String key, GidAsyncApi26Schema definitionAaiSchema) {
        definitionSchemaMap.put(key, definitionAaiSchema);
    }

    public Map<String, GidAsyncApi26Schema> getDefinitionSchemaMap() {
        return definitionSchemaMap;
    }

    public void clearDefinitionSchemaMap() {
        definitionSchemaMap.clear();
    }

    public Collection<AnnotationInstance> getGeneratedClassAnnotations() {
        return generatedClassAnnotations;
    }
}
