package id.global.iris.asyncapi.runtime.scanner;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;

public class AnnotationScannerContext {
    private final FilteredIndexView index;
    private final Aai20Document asyncApi;
    private final Map<String, AaiSchema> definitionSchemaMap;
    private final Collection<AnnotationInstance> generatedClassAnnotations;

    public AnnotationScannerContext(FilteredIndexView index, Aai20Document asyncApi,
            Collection<AnnotationInstance> generatedClassAnnotations) {
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
}
