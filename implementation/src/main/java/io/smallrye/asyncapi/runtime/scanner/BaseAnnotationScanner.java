package io.smallrye.asyncapi.runtime.scanner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.common.annotations.amqp.GlobalIdGenerated;
import id.global.common.annotations.amqp.Scope;
import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20ChannelBindings;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20ChannelItem;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Components;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20HeaderItem;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Message;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Operation;
import io.apicurio.datamodels.core.models.Extension;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.io.schema.SchemaReader;
import io.smallrye.asyncapi.runtime.scanner.model.AaiSchemaAdditionalProperties;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelBindingsInfo;
import io.smallrye.asyncapi.runtime.scanner.model.ChannelInfo;
import io.smallrye.asyncapi.runtime.scanner.model.GidAai20AmqpChannelBindings;
import io.smallrye.asyncapi.runtime.scanner.model.JsonSchemaInfo;

public abstract class BaseAnnotationScanner {

    public static final String PROP_ID = "id";
    public static final String PROP_INFO = "info";
    public static final String PROP_SERVERS = "servers";
    public static final String PROP_CHANNELS = "channels";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";
    public static final String PROP_MESSAGE_SCOPE = "scope";
    private static final String ROLES_ALLOWED = "rolesAllowed";

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

        final var generatedClassAnnotations = filteredIndexView.getAnnotations(
                DotName.createSimple(GlobalIdGenerated.class.getName()));

        this.annotationScannerContext = new AnnotationScannerContext(config, filteredIndexView, new Aai20Document(),
                generatedClassAnnotations);
    }

    public abstract Aai20Document scan();

    protected boolean annotatedMethods(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.METHOD);
    }

    protected boolean annotatedClasses(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.CLASS);
    }

    protected void createChannels(List<ChannelInfo> channelInfos, Map<String, Scope> messageScopes, Aai20Document asyncApi) {
        if (asyncApi.channels == null) {
            asyncApi.channels = new HashMap<>();
        }

        channelInfos.forEach(channelInfo -> {
            String eventKey = channelInfo.getEventKey();
            AaiChannelItem channelItem = new Aai20ChannelItem(eventKey);
            AaiOperation operation;
            if (channelInfo.getOperationType().equals(OperationConstant.PROP_SUBSCRIBE)) {
                channelItem.subscribe = new Aai20Operation(OperationConstant.PROP_SUBSCRIBE);
                operation = channelItem.subscribe;
            } else if (channelInfo.getOperationType().equals(OperationConstant.PROP_PUBLISH)) {
                channelItem.publish = new Aai20Operation(OperationConstant.PROP_PUBLISH);
                operation = channelItem.publish;
            } else {
                throw new IllegalArgumentException("opType argument should be one of [publish, subscribe]");
            }

            operation.message = new Aai20Message(eventKey);
            operation.message.addExtraProperty(PROP_MESSAGE_SCOPE, messageScopes.get(eventKey));
            Extension rolesAllowedExtension = new Extension();
            rolesAllowedExtension.name = ROLES_ALLOWED;
            rolesAllowedExtension.value = channelInfo.getRolesAllowed();

            operation.message.headers = new Aai20HeaderItem();
            operation.message.headers.addExtension(ROLES_ALLOWED, rolesAllowedExtension);
            operation.message._name = eventKey;
            operation.message.name = eventKey;
            operation.message.title = eventKey;

            Schema payloadSchema = new Schema();
            payloadSchema.setReference(COMPONENTS_SCHEMAS_PREFIX + eventKey);
            operation.message.payload = payloadSchema;

            channelItem.bindings = new Aai20ChannelBindings();
            channelItem.bindings.amqp = new GidAai20AmqpChannelBindings();
            GidAai20AmqpChannelBindings amqp = (GidAai20AmqpChannelBindings) channelItem.bindings.amqp;
            amqp.setIs("routingKey"); // We probably won't ever use "queue" ?

            Map<String, Object> queue = new HashMap<>();
            ChannelBindingsInfo bindingsInfo = channelInfo.getBindingsInfo();
            String queueName = bindingsInfo.getQueue();
            queue.put("name", queueName);
            queue.put("durable", bindingsInfo.isQueueDurable());
            queue.put("exclusive", bindingsInfo.isQueueExclusive());
            queue.put("autoDelete", bindingsInfo.isQueueAutoDelete());
            queue.put("vhost", bindingsInfo.getQueueVhost());
            amqp.setQueue(queue);

            Map<String, Object> exchange = new HashMap<>();
            String exchangeName = bindingsInfo.getExchange();
            exchange.put("name", exchangeName);
            exchange.put("type", bindingsInfo.getExchangeType().getType());
            exchange.put("durable", bindingsInfo.isExchangeDurable());
            exchange.put("autoDelete", bindingsInfo.isExchangeAutoDelete());
            exchange.put("vhost", bindingsInfo.getExchangeVhost());
            amqp.setExchange(exchange);

            String channelKey = String.format("%s/%s", exchangeName, queueName);

            asyncApi.channels.put(channelKey, channelItem);
        });
    }

    protected void insertComponentSchemas(final AnnotationScannerContext context, Map<String, JsonSchemaInfo> collectedNodes,
            Aai20Document asyncApi) {
        if (asyncApi.components == null) {
            asyncApi.components = new Aai20Components();
        }
        collectedNodes.forEach((s, jsonSchemaInfo) -> {
            // Read and save definitions of each scanned schema node, if any
            // These definitions are later inserted under `#/components/schemas`
            JsonNode definitions = jsonSchemaInfo.getGeneratedSchema().get("definitions");
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
            AaiSchema aaiSchema = SchemaReader.readSchema(jsonSchemaInfo.getGeneratedSchema(), true);

            aaiSchema.additionalProperties = new AaiSchemaAdditionalProperties(jsonSchemaInfo.isGeneratedClass());

            asyncApi.components.schemas.put(s, aaiSchema);
        });
    }

    protected boolean isGeneratedClass(ClassInfo eventClass) {
        return annotationScannerContext.getGeneratedClassAnnotations().stream()
                .filter(annotationInstance -> annotationInstance.target().kind().equals(AnnotationTarget.Kind.CLASS))
                .anyMatch(annotationInstance -> annotationInstance.target().asClass().name().equals(eventClass.name()));
    }
}
