package io.smallrye.asyncapi.runtime.scanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.IndexView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20ChannelItem;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Message;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Operation;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.io.schema.SchemaReader;

public abstract class BaseAnnotationScanner {

    public static final String PROP_ID = "id";
    public static final String PROP_INFO = "info";
    public static final String PROP_SERVERS = "servers";
    public static final String PROP_CHANNELS = "channels";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";
    public static final String PROP_MESSAGE_TYPE = "type";

    protected final AnnotationScannerContext annotationScannerContext;
    protected ClassLoader classLoader = null;

    public BaseAnnotationScanner(AsyncApiConfig config, IndexView index, ClassLoader classLoader) {
        this(config, index);
        this.classLoader = classLoader;
    }

    public BaseAnnotationScanner(AsyncApiConfig config, IndexView index) {
        FilteredIndexView filteredIndexView;
        if (index instanceof FilteredIndexView) {
            filteredIndexView = FilteredIndexView.class.cast(index);
        } else {
            filteredIndexView = new FilteredIndexView(index, config);
        }
        this.annotationScannerContext = new AnnotationScannerContext(config, filteredIndexView, new Aai20Document());
    }

    public abstract Aai20Document scan();

    protected boolean annotatedMethods(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.METHOD);
    }

    protected boolean annotatedClasses(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.CLASS);
    }

    protected void createChannels(Map<String, ObjectNode> events,
            Map<String, String> messageTypes, String opType, Aai20Document asyncApi) {
        if (asyncApi.channels == null) {
            asyncApi.channels = new HashMap<>();
        }

        events.forEach((eventKey, jsonNodes) -> {
            AaiChannelItem channelItem = new Aai20ChannelItem(eventKey);
            AaiOperation operation;
            if (opType.equals(OperationConstant.PROP_SUBSCRIBE)) {
                channelItem.subscribe = new Aai20Operation(opType);
                operation = channelItem.subscribe;
            } else if (opType.equals(OperationConstant.PROP_PUBLISH)) {
                channelItem.publish = new Aai20Operation(opType);
                operation = channelItem.publish;
            } else {
                throw new IllegalArgumentException("opType argument should be one of [publish, subscribe]");
            }
            operation.message = new Aai20Message(eventKey);
            operation.message.addExtraProperty(PROP_MESSAGE_TYPE, messageTypes.get(eventKey));

            Schema payloadSchema = new Schema();
            payloadSchema.setReference(COMPONENTS_SCHEMAS_PREFIX + eventKey);
            operation.message.payload = payloadSchema;
            asyncApi.channels.put(eventKey, channelItem);
        });
    }

    protected void insertComponentSchemas(final AnnotationScannerContext context, Map<String, ObjectNode> collectedNodes,
            Aai20Document asyncApi) {
        collectedNodes.forEach((s, jsonNodes) -> {
            // Read and save definitions of each scanned schema node, if any
            // These definitions are later inserted under `#/components/schemas`
            JsonNode definitions = jsonNodes.get("definitions");
            if (definitions != null) {
                // extract this to a method
                Iterator<Map.Entry<String, JsonNode>> defFieldsIterator = definitions.fields();
                while (defFieldsIterator.hasNext()) {
                    Map.Entry<String, JsonNode> definition = defFieldsIterator.next();

                    String key = definition.getKey();
                    AaiSchema definitionAaiSchema = SchemaReader.readSchema(definition.getValue(), true);
                    context.addDefinitionSchema(key, definitionAaiSchema);
                }
            }
            AaiSchema aaiSchema = SchemaReader.readSchema(jsonNodes, true);
            asyncApi.components.schemas.put(s, aaiSchema);
        });
    }
}
