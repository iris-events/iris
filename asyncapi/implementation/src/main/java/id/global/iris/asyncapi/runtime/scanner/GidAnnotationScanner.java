/*
 * Copyright 2019 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package id.global.iris.asyncapi.runtime.scanner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import id.global.iris.asyncapi.api.AsyncApiConfig;
import id.global.iris.asyncapi.runtime.generator.CustomDefinitionProvider;
import id.global.iris.asyncapi.runtime.io.components.ComponentReader;
import id.global.iris.asyncapi.runtime.scanner.model.ChannelInfo;
import id.global.iris.asyncapi.runtime.scanner.model.JsonSchemaInfo;
import id.global.iris.asyncapi.runtime.scanner.validator.MessageAnnotationValidator;
import id.global.iris.asyncapi.runtime.util.ChannelInfoGenerator;
import id.global.iris.asyncapi.runtime.util.SchemeIdGenerator;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.annotations.Scope;
import id.global.iris.common.annotations.SnapshotMessageHandler;
import id.global.iris.common.constants.HandlerDefaultParameter;
import id.global.iris.parsers.BindingKeysParser;
import id.global.iris.parsers.DeadLetterQueueParser;
import id.global.iris.parsers.ExchangeParser;
import id.global.iris.parsers.ExchangeTtlParser;
import id.global.iris.parsers.ExchangeTypeParser;
import id.global.iris.parsers.MessageScopeParser;
import id.global.iris.parsers.PersistentParser;
import id.global.iris.parsers.QueueAutoDeleteParser;
import id.global.iris.parsers.QueueDurableParser;
import id.global.iris.parsers.ResourceTypeParser;
import id.global.iris.parsers.ResponseParser;
import id.global.iris.parsers.RolesAllowedParser;
import id.global.iris.parsers.RoutingKeyParser;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans a deployment (using the archive and jandex annotation index) for relevant annotations. These
 * annotations, if found, are used to generate a valid AsyncAPI model.
 *
 * @author gasper.vrhovsek@gmail.com
 */
public class GidAnnotationScanner extends BaseAnnotationScanner {
    private static final Logger LOG = LoggerFactory.getLogger(GidAnnotationScanner.class);

    private final SchemaGenerator schemaGenerator;
    private final String projectName;
    private final String projectGroupId;
    private final String projectVersion;

    public static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getName());
    public static final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getName());
    public static final DotName DOT_NAME_SNAPSHOT_MESSAGE_HANDLER = DotName
            .createSimple(SnapshotMessageHandler.class.getName());

    /**
     * Constructor.
     *
     * @param config AsyncApiConfig instance
     * @param index IndexView of deployment
     * @param projectName Name of project
     */
    public GidAnnotationScanner(AsyncApiConfig config, IndexView index, String projectName, String projectGroupId,
            String projectVersion) {
        super(config, index);
        this.schemaGenerator = initSchemaGenerator(config);
        this.projectName = projectName;
        this.projectGroupId = projectGroupId;
        this.projectVersion = projectVersion;
    }

    public GidAnnotationScanner(AsyncApiConfig config, IndexView index, ClassLoader classLoader, String projectName,
            String projectGroupId,
            String projectVersion) {
        super(config, index, classLoader);
        this.schemaGenerator = initSchemaGenerator(config);
        this.projectName = projectName;
        this.projectGroupId = projectGroupId;
        this.projectVersion = projectVersion;
    }

    /**
     * Scan the deployment for relevant annotations. Returns an AsyncAPI data model that was
     * built from those found annotations.
     *
     * @return Document generated from scanning annotations
     */
    public Aai20Document scan() {
        LOG.debug("Scanning deployment for Async Annotations.");
        try {
            return scanIrisAnnotations();
        } catch (ClassNotFoundException e) {
            LOG.error("Could not create AaiDocument", e);
            throw new RuntimeException("Could not create AaiDocument", e);
        }
    }

    private Aai20Document scanIrisAnnotations() throws ClassNotFoundException {
        final var asyncApi = this.annotationScannerContext.getAsyncApi();
        setDocumentInfo(asyncApi);
        // Process @Message
        final var messageAnnotations = getMessageAnnotations(this.annotationScannerContext.getIndex()).collect(
                Collectors.toList());
        LOG.debug(String.format("Got %s message annotations", messageAnnotations.size()));

        final var validator = new MessageAnnotationValidator();
        validator.validateReservedNames(messageAnnotations, this.projectName, this.projectGroupId);

        final var handlerAnnotations = getHandlerAnnotations(annotationScannerContext.getIndex(),
                List.of(DOT_NAME_MESSAGE_HANDLER, DOT_NAME_SNAPSHOT_MESSAGE_HANDLER));
        final var consumedMessageAnnotations = processMessageHandlerAnnotations(handlerAnnotations,
                annotationScannerContext, asyncApi);

        LOG.debug(String.format("Got %s consumed message annotations", consumedMessageAnnotations.size()));

        processProducedMessages(annotationScannerContext, asyncApi, messageAnnotations, consumedMessageAnnotations);
        processContextDefinitionReferencedSchemas(annotationScannerContext, asyncApi);

        return asyncApi;
    }

    private List<AnnotationInstance> getHandlerAnnotations(IndexView index, List<DotName> handlerDotNames) {
        return handlerDotNames.stream()
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .filter(this::annotatedMethods)
                .toList();
    }

    private Stream<AnnotationInstance> getMessageAnnotations(IndexView index) {
        final var annotationName = DotName.createSimple(Message.class.getName());
        return index.getAnnotations(annotationName).stream().filter(this::annotatedClasses);
    }

    private void processProducedMessages(AnnotationScannerContext context,
            Aai20Document asyncApi,
            List<AnnotationInstance> messageAnnotations,
            List<AnnotationInstance> consumedMessageAnnotations)
            throws ClassNotFoundException {
        final var index = context.getIndex();
        messageAnnotations.removeAll(consumedMessageAnnotations);

        final var channelInfos = new ArrayList<ChannelInfo>();
        final var producedMessages = new HashMap<String, JsonSchemaInfo>();
        final var messageScopes = new HashMap<String, Scope>();
        for (AnnotationInstance anno : messageAnnotations) {
            final var classInfo = anno.target().asClass();
            final var classSimpleName = classInfo.simpleName();

            messageScopes.put(classSimpleName, MessageScopeParser.getFromAnnotationInstance(anno, index));
            producedMessages.put(classSimpleName, generateProducedMessageSchemaInfo(classInfo));

            final var routingKey = RoutingKeyParser.getFromAnnotationInstance(anno);
            final var exchangeType = ExchangeTypeParser.getFromAnnotationInstance(anno, index);
            final var exchange = ExchangeParser.getFromAnnotationInstance(anno);

            // Header values
            final var rolesAllowed = RolesAllowedParser.getFromAnnotationInstance(anno, index);
            final var deadLetterQueue = DeadLetterQueueParser.getFromAnnotationInstance(anno, index);
            final var ttl = ExchangeTtlParser.getFromAnnotationInstance(anno, index);
            final var persistent = PersistentParser.getFromAnnotationInstance(anno, index);

            channelInfos.add(ChannelInfoGenerator.generateSubscribeChannelInfo(
                    exchange,
                    routingKey,
                    classSimpleName,
                    exchangeType,
                    rolesAllowed,
                    deadLetterQueue,
                    ttl,
                    persistent));
        }

        insertComponentSchemas(context, producedMessages, asyncApi);

        // TODO check what's with the types

        createChannels(channelInfos, messageScopes, asyncApi);
    }

    private List<AnnotationInstance> processMessageHandlerAnnotations(List<AnnotationInstance> methodAnnotationInstances,
            AnnotationScannerContext context, Aai20Document asyncApi)
            throws ClassNotFoundException {

        final var consumedMessages = new ArrayList<AnnotationInstance>();
        final var index = context.getIndex();
        final var incomingMessages = new HashMap<String, JsonSchemaInfo>();
        final var channelInfos = new ArrayList<ChannelInfo>();
        final var messageTypes = new HashMap<String, Scope>();

        for (AnnotationInstance handlerAnnotation : methodAnnotationInstances) {

            final var annotationName = handlerAnnotation.name();
            final var annotationValues = handlerAnnotation.values();
            final var methodInfo = (MethodInfo) handlerAnnotation.target();
            final var methodParameters = methodInfo.parameters();

            final var messageAnnotation = getMessageAnnotation(methodParameters, index);
            consumedMessages.add(messageAnnotation);

            final var messageClass = messageAnnotation.target().asClass();
            final var messageClassSimpleName = messageClass.simpleName();

            final var bindingKeys = getBindingKeys(handlerAnnotation, messageAnnotation);
            final var exchangeType = ExchangeTypeParser.getFromAnnotationInstance(messageAnnotation, index);
            final var exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
            final var scope = MessageScopeParser.getFromAnnotationInstance(messageAnnotation, index);

            final var durable = getDurable(handlerAnnotation, index);
            final var autoDelete = getAutoDelete(handlerAnnotation, index);
            final var deadLetterQueue = DeadLetterQueueParser.getFromAnnotationInstance(messageAnnotation, index);
            final var ttl = ExchangeTtlParser.getFromAnnotationInstance(messageAnnotation, index);
            final var persistent = PersistentParser.getFromAnnotationInstance(messageAnnotation, index);

            final var responseType = ResponseParser.getFromAnnotationInstance(messageAnnotation, index);

            final var isGeneratedClass = isGeneratedClass(messageClass);

            final var jsonSchemaInfo = generateJsonSchemaInfo(
                    annotationName,
                    messageClass.name().toString(),
                    annotationValues,
                    isGeneratedClass);

            final var subscribeChannelInfo = ChannelInfoGenerator.generatePublishChannelInfo(
                    exchange,
                    bindingKeys,
                    messageClassSimpleName,
                    exchangeType,
                    durable,
                    autoDelete,
                    RolesAllowedParser.getFromAnnotationInstance(handlerAnnotation, index),
                    deadLetterQueue,
                    ttl,
                    responseType,
                    persistent);

            messageTypes.put(messageClassSimpleName, scope);
            incomingMessages.put(messageClassSimpleName, jsonSchemaInfo);
            channelInfos.add(subscribeChannelInfo);
        }

        insertComponentSchemas(context, incomingMessages, asyncApi);
        createChannels(channelInfos, messageTypes, asyncApi);

        return consumedMessages;
    }

    private String getBindingKeys(AnnotationInstance handlerAnnotationInstance, AnnotationInstance messageAnnotation) {
        final var dotName = handlerAnnotationInstance.name();
        if (DOT_NAME_SNAPSHOT_MESSAGE_HANDLER.equals(dotName)) {
            return ResourceTypeParser.getFromAnnotationInstance(handlerAnnotationInstance);
        } else if (DOT_NAME_MESSAGE_HANDLER.equals(dotName)) {
            return BindingKeysParser.getFromAnnotationInstanceAsCsv(handlerAnnotationInstance, messageAnnotation);
        }
        throw new IllegalArgumentException("Unsupported annotation instance " + dotName);
    }

    private boolean getDurable(AnnotationInstance handlerAnnotationInstance, FilteredIndexView indexView) {
        final var dotName = handlerAnnotationInstance.name();
        if (DOT_NAME_SNAPSHOT_MESSAGE_HANDLER.equals(dotName)) {
            return HandlerDefaultParameter.SnapshotMessageHandler.DURABLE;
        } else if (DOT_NAME_MESSAGE_HANDLER.equals(dotName)) {
            return QueueDurableParser.getFromAnnotationInstance(handlerAnnotationInstance, indexView);
        }
        throw new IllegalArgumentException("Unsupported annotation instance " + dotName);
    }

    private boolean getAutoDelete(AnnotationInstance handlerAnnotationInstance, FilteredIndexView indexView) {
        final var dotName = handlerAnnotationInstance.name();
        if (DOT_NAME_SNAPSHOT_MESSAGE_HANDLER.equals(dotName)) {
            return HandlerDefaultParameter.SnapshotMessageHandler.AUTO_DELETE;
        } else if (DOT_NAME_MESSAGE_HANDLER.equals(dotName)) {
            return QueueAutoDeleteParser.getFromAnnotationInstance(handlerAnnotationInstance, indexView);
        }
        throw new IllegalArgumentException("Unsupported annotation instance " + dotName);
    }

    private void processContextDefinitionReferencedSchemas(AnnotationScannerContext context, Aai20Document asyncApi) {
        final var definitionSchemaMap = context.getDefinitionSchemaMap();
        asyncApi.components.schemas.putAll(definitionSchemaMap);
        context.clearDefinitionSchemaMap();
    }

    private Aai20Document setDocumentInfo(Aai20Document document) {
        try {
            final var projectSchemaId = SchemeIdGenerator.buildId(projectName);

            final var info = new Aai20Info();
            info.title = projectName;
            info.version = this.projectVersion;

            document.id = projectSchemaId;
            document.info = info;
            document.components = ComponentReader.create();
            return document;
        } catch (URISyntaxException e) {
            LOG.error("Could not generate schema ID", e);
            throw new RuntimeException(e);
        }
    }

    private JsonSchemaInfo generateProducedMessageSchemaInfo(ClassInfo classInfo) throws ClassNotFoundException {
        final var className = classInfo.name().toString();
        final var loadedClass = loadClass(className);
        final var classSimpleName = loadedClass.getSimpleName();
        final var isGeneratedClass = isGeneratedClass(classInfo);

        final var generatedSchema = schemaGenerator.generateSchema(loadedClass);
        return new JsonSchemaInfo(
                null,
                classSimpleName,
                generatedSchema,
                null,
                isGeneratedClass);
    }

    private JsonSchemaInfo generateJsonSchemaInfo(DotName annotationName, String className,
            List<AnnotationValue> annotationValues, boolean isGeneratedClass) throws ClassNotFoundException {
        final var loadedClass = loadClass(className);
        final var eventSimpleName = loadedClass.getSimpleName();
        final var generatedSchema = schemaGenerator.generateSchema(loadedClass);
        return new JsonSchemaInfo(
                annotationName,
                eventSimpleName,
                generatedSchema,
                annotationValues,
                isGeneratedClass);
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        if (classLoader != null) {
            return classLoader.loadClass(className);
        } else {
            return Class.forName(className);
        }
    }

    private SchemaGenerator initSchemaGenerator(AsyncApiConfig config) {
        // Schema generator JsonSchema of components
        final var module = new JacksonModule(
                JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY);
        final var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7,
                OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(module);

        configBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider(
                        CustomDefinitionProvider.convertTypesToObject(config.excludeFromSchemas()));

        configBuilder.forFields()
                .withCustomDefinitionProvider(
                        CustomDefinitionProvider.convertFieldsToObject(config.excludeFromSchemas()));

        configBuilder.with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES);
        return new SchemaGenerator(configBuilder.build());
    }

    private AnnotationInstance getMessageAnnotation(final List<Type> parameters,
            final FilteredIndexView index) {

        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .map(classInfo -> classInfo.classAnnotation(DOT_NAME_MESSAGE))
                .filter(Objects::nonNull).toList();

        if (consumedEventTypes.isEmpty()) {
            throw new IllegalArgumentException("Consumed Event not found");
        }

        if (consumedEventTypes.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple consumed Events detected. Message handler can only handle one event type.");
        }

        return consumedEventTypes.get(0);
    }
}
