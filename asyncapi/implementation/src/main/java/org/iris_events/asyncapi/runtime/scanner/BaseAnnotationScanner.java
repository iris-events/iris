package org.iris_events.asyncapi.runtime.scanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.api.AsyncApiConfig;
import org.iris_events.asyncapi.runtime.io.channel.operation.OperationConstant;
import org.iris_events.asyncapi.runtime.io.schema.SchemaReader;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelInfo;
import org.iris_events.asyncapi.runtime.scanner.model.GidAaiAMQPOperationBinding;
import org.iris_events.asyncapi.runtime.scanner.model.GidAsyncApi26MessageImpl;
import org.iris_events.asyncapi.runtime.scanner.model.GidAsyncApi26Schema;
import org.iris_events.asyncapi.runtime.scanner.model.JsonSchemaInfo;
import org.iris_events.asyncapi.runtime.util.HeaderSchemaBuilder;
import org.iris_events.common.DeliveryMode;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperation;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26BindingImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ChannelBindingsImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Schema;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26SchemaImpl;

public abstract class BaseAnnotationScanner {

    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";

    protected final AnnotationScannerContext annotationScannerContext;
    protected ClassLoader classLoader = null;
    protected ObjectMapper objectMapper;
    protected final HeaderSchemaBuilder headerSchemaBuilder;

    public BaseAnnotationScanner(AsyncApiConfig config, IndexView index, ClassLoader classLoader, ObjectMapper objectMapper) {
        this(config, index, objectMapper);
        this.classLoader = classLoader;
    }

    public BaseAnnotationScanner(AsyncApiConfig config, IndexView index, ObjectMapper objectMapper) {
        FilteredIndexView filteredIndexView;
        if (index instanceof FilteredIndexView) {
            filteredIndexView = (FilteredIndexView) index;
        } else {
            filteredIndexView = new FilteredIndexView(index, config);
        }

        final var generatedClassAnnotations = filteredIndexView.getAnnotations(
                DotName.createSimple(IrisGenerated.class.getName()));
        this.objectMapper = objectMapper;

        this.annotationScannerContext = new AnnotationScannerContext(filteredIndexView, getDocument(),
                generatedClassAnnotations);
        this.headerSchemaBuilder = new HeaderSchemaBuilder(objectMapper);
    }

    private static AsyncApi26Document getDocument() {
        final var document = (AsyncApi26Document) Library.createDocument(ModelType.ASYNCAPI26);
        document.setAsyncapi("2.6.0");
        document.setDefaultContentType("application/json");
        return document;
    }

    public abstract AsyncApi26Document scan();

    protected boolean annotatedMethods(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.METHOD);
    }

    protected boolean annotatedClasses(AnnotationInstance annotation) {
        return Objects.equals(annotation.target().kind(), AnnotationTarget.Kind.CLASS);
    }

    protected void createChannels(List<ChannelInfo> channelInfos, Map<String, Scope> messageScopes,
            AsyncApi26Document document) {

        final var channels = document.getChannels() == null ? document.createChannels() : document.getChannels();
        document.setChannels(channels);

        channelInfos.forEach(channelInfo -> {
            String messageKey = channelInfo.getEventKey();

            AsyncApiChannelItem channelItem = channels.createChannelItem();

            var operation = channelItem.createOperation();

            final var persistent = channelInfo.getOperationBindingsInfo().persistent();
            final var deliveryMode = getDeliveryMode(persistent);
            final var aaiAMQPOperationBinding = new GidAaiAMQPOperationBinding();
            aaiAMQPOperationBinding.setDeliveryMode(deliveryMode);

            final var operationBindings = operation.createOperationBindings();
            operationBindings.setAmqp(aaiAMQPOperationBinding);
            operation.setBindings(operationBindings);

            final var message = new GidAsyncApi26MessageImpl();
            message.setHeaders(headerSchemaBuilder.buildHeaders(channelInfo, messageScopes));
            message.setName(messageKey);
            message.setTitle(messageKey);
            message.setPayload(getRefJsonNode(messageKey));

            operation.setMessage(message);
            setResponseType(channelInfo, operation);

            if (channelInfo.getOperationType().equals(OperationConstant.PROP_SUBSCRIBE)) {
                channelItem.setSubscribe(operation);
            } else if (channelInfo.getOperationType().equals(OperationConstant.PROP_PUBLISH)) {
                channelItem.setPublish(operation);
            } else {
                throw new IllegalArgumentException("opType argument should be one of [publish, subscribe]");
            }

            final var amqpChannelBindings = new AsyncApi26BindingImpl();

            String queueName = channelInfo.getBindingsInfo().getQueue();
            String exchangeName = channelInfo.getBindingsInfo().getExchange();

            amqpChannelBindings.addExtension("is", TextNode.valueOf("routingKey"));
            amqpChannelBindings.addExtension("queue", getQueueBindings(queueName, channelInfo));
            amqpChannelBindings.addExtension("exchange", getExchangeBindings(exchangeName, channelInfo));

            final var channelBindings = new AsyncApi26ChannelBindingsImpl();
            channelBindings.setAmqp(amqpChannelBindings);
            channelItem.setBindings(channelBindings);

            String channelKey = String.format("%s/%s", exchangeName, queueName);
            channels.addItem(channelKey, channelItem);
        });

    }

    private <T extends JsonNode> T getExchangeBindings(final String exchangeName, final ChannelInfo channelInfo) {
        final var bindingsInfo = channelInfo.getBindingsInfo();

        Map<String, Object> exchange = new HashMap<>();
        exchange.put("name", exchangeName);
        exchange.put("type", bindingsInfo.getExchangeType().getType());
        exchange.put("durable", bindingsInfo.isExchangeDurable());
        exchange.put("autoDelete", bindingsInfo.isExchangeAutoDelete());
        exchange.put("vhost", bindingsInfo.getExchangeVhost());
        return objectMapper.valueToTree(exchange);
    }

    private <T extends JsonNode> T getQueueBindings(final String queueName, final ChannelInfo channelInfo) {
        final var bindingsInfo = channelInfo.getBindingsInfo();

        Map<String, Object> queue = new HashMap<>();
        queue.put("name", queueName);
        queue.put("durable", bindingsInfo.isQueueDurable());
        queue.put("exclusive", bindingsInfo.isQueueExclusive());
        queue.put("autoDelete", bindingsInfo.isQueueAutoDelete());
        queue.put("vhost", bindingsInfo.getQueueVhost());
        return objectMapper.valueToTree(queue);
    }

    private JsonNode getRefJsonNode(String eventName) {
        try {
            return objectMapper.readTree(String.format("{\"$ref\" : \"#/components/schemas/%s\"}", eventName));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not read ref json node", e);
        }
    }

    private int getDeliveryMode(final boolean persistent) {
        return persistent ? DeliveryMode.PERSISTENT.getValue() : DeliveryMode.NON_PERSISTENT.getValue();
    }

    private void setResponseType(final ChannelInfo channelInfo, final AsyncApiOperation operation) {
        final var responseType = channelInfo.getResponseType();
        if (responseType == null) {
            return;
        }

        DotName responseTypeName = responseType.asClassType().name();
        if (responseTypeName.toString().equals(Void.class.getName())) {
            return;
        }

        AsyncApi26Schema responseSchema = new AsyncApi26SchemaImpl();
        responseSchema.set$ref(COMPONENTS_SCHEMAS_PREFIX + responseTypeName.local());
        ((GidAsyncApi26MessageImpl) operation.getMessage()).response = responseSchema;
    }

    protected void insertComponentSchemas(final AnnotationScannerContext context, Map<String, JsonSchemaInfo> collectedNodes,
            AsyncApi26Document asyncApi) {
        if (asyncApi.getComponents() == null) {
            asyncApi.setComponents(new AsyncApi26ComponentsImpl());
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
                    GidAsyncApi26Schema definitionAaiSchema = SchemaReader.readSchema(definition.getValue(), true);
                    context.addDefinitionSchema(key, definitionAaiSchema);
                }
            }
            GidAsyncApi26Schema aaiSchema = SchemaReader.readSchema(jsonSchemaInfo.getGeneratedSchema(), true);
            aaiSchema.addExtension("x-iris-generated", BooleanNode.valueOf(jsonSchemaInfo.isGeneratedClass()));

            Optional.ofNullable(jsonSchemaInfo.getCacheTtl()).ifPresent(cacheTtl -> {
                aaiSchema.addExtension("x-cached-message-ttl-seconds", IntNode.valueOf(cacheTtl));
            });

            asyncApi.getComponents().addSchema(s, aaiSchema);
        });
    }

    protected boolean isGeneratedClass(ClassInfo eventClass) {
        return annotationScannerContext.getGeneratedClassAnnotations().stream()
                .filter(annotationInstance -> annotationInstance.target().kind().equals(AnnotationTarget.Kind.CLASS))
                .anyMatch(annotationInstance -> annotationInstance.target().asClass().name().equals(eventClass.name()));
    }
}
