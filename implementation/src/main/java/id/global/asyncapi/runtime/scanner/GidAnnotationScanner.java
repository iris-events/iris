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

package id.global.asyncapi.runtime.scanner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import id.global.amqp.parsers.BindingKeysParser;
import id.global.amqp.parsers.DeadLetterQueueParser;
import id.global.amqp.parsers.ExchangeParser;
import id.global.amqp.parsers.ExchangeTtlParser;
import id.global.amqp.parsers.ExchangeTypeParser;
import id.global.amqp.parsers.MessageScopeParser;
import id.global.amqp.parsers.QueueAutoDeleteParser;
import id.global.amqp.parsers.QueueDurableParser;
import id.global.amqp.parsers.RolesAllowedParser;
import id.global.amqp.parsers.RoutingKeyParser;
import id.global.asyncapi.api.AsyncApiConfig;
import id.global.asyncapi.api.util.MergeUtil;
import id.global.asyncapi.runtime.generator.CustomDefinitionProvider;
import id.global.asyncapi.runtime.io.components.ComponentReader;
import id.global.asyncapi.runtime.io.info.InfoReader;
import id.global.asyncapi.runtime.io.server.ServerReader;
import id.global.asyncapi.runtime.scanner.model.ChannelInfo;
import id.global.asyncapi.runtime.scanner.model.JsonSchemaInfo;
import id.global.asyncapi.runtime.util.ChannelInfoGenerator;
import id.global.asyncapi.runtime.util.JandexUtil;
import id.global.asyncapi.runtime.util.SchemeIdGenerator;
import id.global.asyncapi.spec.annotations.EventApp;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.annotations.amqp.Scope;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;

/**
 * Scans a deployment (using the archive and jandex annotation index) for relevant annotations. These
 * annotations, if found, are used to generate a valid AsyncAPI model.
 *
 * @author gasper.vrhovsek@gmail.com
 */
public class GidAnnotationScanner extends BaseAnnotationScanner {
    private static final Logger LOG = Logger.getLogger(GidAnnotationScanner.class);

    public static final DotName DOTNAME_EVENT_APP_DEFINITION = DotName.createSimple(EventApp.class.getName());
    public static final DotName DOTNAME_MESSAGE = DotName.createSimple(Message.class.getName());
    private final SchemaGenerator schemaGenerator;

    /**
     * Constructor.
     *
     * @param config AsyncApiConfig instance
     * @param index  IndexView of deployment
     */
    public GidAnnotationScanner(AsyncApiConfig config, IndexView index) {
        super(config, index);
        schemaGenerator = initSchemaGenerator(config);
    }

    public GidAnnotationScanner(AsyncApiConfig config, IndexView index, ClassLoader classLoader) {
        super(config, index, classLoader);
        schemaGenerator = initSchemaGenerator(config);
    }

    /**
     * Scan the deployment for relevant annotations. Returns an AsyncAPI data model that was
     * built from those found annotations.
     *
     * @return Document generated from scanning annotations
     */
    public Aai20Document scan() {
        LOG.debug("Scanning deployment for Async Annotations.");
        Aai20Document messageHandlerAaiDocument;
        try {
            messageHandlerAaiDocument = scanGidEventAppAnnotations();
        } catch (ClassNotFoundException e) {
            LOG.error("Could not create AaiDocument", e);
            throw new RuntimeException("Could not create AaiDocument", e);
        }
        return messageHandlerAaiDocument;
    }

    private Aai20Document scanGidEventAppAnnotations() throws ClassNotFoundException {
        Aai20Document asyncApi = this.annotationScannerContext.getAsyncApi();
        // Process @EventApp
        processEventAppDefinition(annotationScannerContext, asyncApi);
        // Process @Message
        List<AnnotationInstance> allMessageAnnotations = getClassAnnotations(this.annotationScannerContext.getIndex()).collect(
                Collectors.toList());

        // Process @MessageHandler
        List<AnnotationInstance> consumedMessageAnnotations = processMessageHandlerAnnotations(annotationScannerContext,
                asyncApi);

        processProducedMessages(annotationScannerContext, asyncApi, allMessageAnnotations, consumedMessageAnnotations);
        processContextDefinitionReferencedSchemas(annotationScannerContext, asyncApi);

        return asyncApi;
    }

    private Stream<AnnotationInstance> getMethodAnnotations(IndexView index) {
        DotName annotationName = DotName.createSimple(MessageHandler.class.getName());
        return index.getAnnotations(annotationName).stream().filter(this::annotatedMethods);
    }

    private Stream<AnnotationInstance> getClassAnnotations(IndexView index) {
        DotName annotationName = DotName.createSimple(Message.class.getName());
        return index.getAnnotations(annotationName).stream().filter(this::annotatedClasses);
    }

    private void processProducedMessages(AnnotationScannerContext context,
            Aai20Document asyncApi,
            List<AnnotationInstance> allMessageAnnotations,
            List<AnnotationInstance> consumedMessageAnnotations)
            throws ClassNotFoundException {
        FilteredIndexView index = context.getIndex();
        allMessageAnnotations.removeAll(consumedMessageAnnotations);

        List<ChannelInfo> channelInfos = new ArrayList<>();
        Map<String, JsonSchemaInfo> producedMessages = new HashMap<>();
        Map<String, Scope> messageScopes = new HashMap<>();
        for (AnnotationInstance anno : allMessageAnnotations) {
            ClassInfo classInfo = anno.target().asClass();
            String classSimpleName = classInfo.simpleName();

            messageScopes.put(classSimpleName, MessageScopeParser.getFromAnnotationInstance(anno, index));
            producedMessages.put(classSimpleName, generateProducedMessageSchemaInfo(classInfo));

            final var routingKey = RoutingKeyParser.getFromAnnotationInstance(anno);
            final var exchangeType = ExchangeTypeParser.getFromAnnotationInstance(anno, index);
            final var exchange = ExchangeParser.getFromAnnotationInstance(anno);
            final var rolesAllowed = RolesAllowedParser.getFromAnnotationInstance(anno, index);
            final var deadLetterQueue = DeadLetterQueueParser.getFromAnnotationInstance(anno, index);
            final var ttl = ExchangeTtlParser.getFromAnnotationInstance(anno, index);

            channelInfos.add(ChannelInfoGenerator.generateSubscribeChannelInfo(
                    exchange,
                    routingKey,
                    classSimpleName,
                    exchangeType,
                    rolesAllowed,
                    deadLetterQueue,
                    ttl));
        }

        insertComponentSchemas(context, producedMessages, asyncApi);

        // TODO check what's with the types

        createChannels(channelInfos, messageScopes, asyncApi);
    }

    private List<AnnotationInstance> processMessageHandlerAnnotations(AnnotationScannerContext context, Aai20Document asyncApi)
            throws ClassNotFoundException {

        List<AnnotationInstance> consumedMessages = new ArrayList<>();

        FilteredIndexView index = context.getIndex();
        final var methodAnnotationInstances = getMethodAnnotations(index).collect(Collectors.toList());

        final var incomingMessages = new HashMap<String, JsonSchemaInfo>();
        final var channelInfos = new ArrayList<ChannelInfo>();
        final var messageTypes = new HashMap<String, Scope>();

        for (AnnotationInstance messageHandlerAnnotation : methodAnnotationInstances) {

            final var annotationName = messageHandlerAnnotation.name();
            final var annotationValues = messageHandlerAnnotation.values();
            final var methodInfo = (MethodInfo) messageHandlerAnnotation.target();
            final var methodParameters = methodInfo.parameters();

            final var messageAnnotation = getMessageAnnotation(methodParameters, index);
            consumedMessages.add(messageAnnotation);

            final var messageClass = messageAnnotation.target().asClass();
            final var messageClassSimpleName = messageClass.simpleName();

            final var bindingKeys = BindingKeysParser.getFromAnnotationInstanceAsCsv(messageHandlerAnnotation, messageAnnotation);
            final var exchangeType = ExchangeTypeParser.getFromAnnotationInstance(messageAnnotation, index);
            final var exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
            final var scope = MessageScopeParser.getFromAnnotationInstance(messageAnnotation, index);

            final var durable = QueueDurableParser.getFromAnnotationInstance(messageHandlerAnnotation, index);
            final var autodelete = QueueAutoDeleteParser.getFromAnnotationInstance(messageHandlerAnnotation, index);
            final var deadLetterQueue = DeadLetterQueueParser.getFromAnnotationInstance(messageAnnotation, index);
            final var ttl = ExchangeTtlParser.getFromAnnotationInstance(messageAnnotation, index);

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
                    autodelete,
                    RolesAllowedParser.getFromAnnotationInstance(messageHandlerAnnotation, index),
                    deadLetterQueue,
                    ttl);

            messageTypes.put(messageClassSimpleName, scope);
            incomingMessages.put(messageClassSimpleName, jsonSchemaInfo);
            channelInfos.add(subscribeChannelInfo);
        }

        insertComponentSchemas(context, incomingMessages, asyncApi);
        createChannels(channelInfos, messageTypes, asyncApi);

        return consumedMessages;
    }

    private void processContextDefinitionReferencedSchemas(AnnotationScannerContext context, Aai20Document asyncApi) {
        Map<String, AaiSchema> definitionSchemaMap = context.getDefinitionSchemaMap();
        asyncApi.components.schemas.putAll(definitionSchemaMap);
        context.clearDefinitionSchemaMap();
    }

    private Aai20Document processEventAppDefinition(final AnnotationScannerContext context, Aai20Document document) {
        final var annotations = context.getIndex().getAnnotations(DOTNAME_EVENT_APP_DEFINITION);
        final var packageDefs = annotations
                .stream()
                .filter(this::annotatedClasses)
                .collect(Collectors.toList());

        String projectVersion = context.getConfig().projectVersion();
        // Here we have packageDefs, now to build the AsyncAPI
        for (AnnotationInstance packageDef : packageDefs) {
            final var packageAai = new Aai20Document();
            try {
                final var projectId = JandexUtil.stringValue(packageDef, PROP_ID);
                final var projectSchemaId = SchemeIdGenerator.buildId(projectId);
                context.setProjectId(projectId);
                packageAai.id = projectSchemaId;
            } catch (URISyntaxException e) {
                LOG.error("Could not generate schema ID", e);
                throw new RuntimeException(e);
            }
            packageAai.info = InfoReader.readInfo(packageDef.value(PROP_INFO));
            if (projectVersion != null) {
                packageAai.info.version = projectVersion;
            }

            packageAai.servers = ServerReader.readServers(packageDef.value(PROP_SERVERS)).orElse(null);
            packageAai.components = ComponentReader.create();
            MergeUtil.merge(document, packageAai);
        }
        return document;
    }

    private JsonSchemaInfo generateProducedMessageSchemaInfo(ClassInfo classInfo) throws ClassNotFoundException {
        final var className = classInfo.name().toString();
        final var loadedClass = loadClass(className);
        final var classSimpleName = loadedClass.getSimpleName();
        final var isGeneratedClass = isGeneratedClass(classInfo);

        ObjectNode generatedSchema = schemaGenerator.generateSchema(loadedClass);
        return new JsonSchemaInfo(
                null,
                classSimpleName,
                generatedSchema,
                null,
                isGeneratedClass);
    }

    private JsonSchemaInfo generateJsonSchemaInfo(DotName annotationName, String className,
            List<AnnotationValue> annotationValues, boolean isGeneratedClass) throws ClassNotFoundException {
        Class<?> loadedClass = loadClass(className);
        String eventSimpleName = loadedClass.getSimpleName();
        ObjectNode generatedSchema = schemaGenerator.generateSchema(loadedClass);
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
        JacksonModule module = new JacksonModule(
                JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY
        );
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7,
                OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(module);

        Set<String> excludeFromSchemas = config.excludeFromSchemas();
        if (!excludeFromSchemas.isEmpty()) {
            LOG.info("Registering custom definition providers for package prefixes: " + excludeFromSchemas);
            configBuilder.forTypesInGeneral()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownTypeToObject(excludeFromSchemas));

            configBuilder.forFields()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownFieldToObject(excludeFromSchemas));
        }

        SchemaGeneratorConfig schemaGeneratorConfig = configBuilder.build();
        return new SchemaGenerator(schemaGeneratorConfig);
    }

    private AnnotationInstance getMessageAnnotation(final List<Type> parameters,
            final FilteredIndexView index) {

        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .map(classInfo -> classInfo.classAnnotation(DOTNAME_MESSAGE))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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
