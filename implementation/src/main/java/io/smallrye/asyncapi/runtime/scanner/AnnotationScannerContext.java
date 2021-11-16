package io.smallrye.asyncapi.runtime.scanner;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.smallrye.asyncapi.api.AsyncApiConfig;

public class AnnotationScannerContext {
    private final AsyncApiConfig config;
    private final FilteredIndexView index;
    private final Aai20Document asyncApi;
    private final Map<String, AaiSchema> definitionSchemaMap;
    private final Collection<AnnotationInstance> generatedClassAnnotations;
    private String projectId;

    public AnnotationScannerContext(AsyncApiConfig config, FilteredIndexView index,
            Aai20Document asyncApi, Collection<AnnotationInstance> generatedClassAnnotations) {
        this.config = config;
        this.index = index;
        this.asyncApi = asyncApi;
        this.generatedClassAnnotations = generatedClassAnnotations;

        this.definitionSchemaMap = new LinkedHashMap<>();
    }

    public Aai20Document getAsyncApi() {
        return this.asyncApi;
    }

    public FilteredIndexView getIndex() {
        return index;
    }

    public AsyncApiConfig getConfig() {
        return config;
    }

    public void addDefinitionSchema(String key, AaiSchema definitionAaiSchema) {
        definitionSchemaMap.put(key, definitionAaiSchema);
    }

    public Map<String, AaiSchema> getDefinitionSchemaMap() {
        return definitionSchemaMap;
    }

    public void clearDefinitionSchemaMap() {
        definitionSchemaMap.clear();
    }

    public Collection<AnnotationInstance> getGeneratedClassAnnotations() {
        return generatedClassAnnotations;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }
}
