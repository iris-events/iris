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

package io.smallrye.asyncapi.runtime.scanner;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.api.util.MergeUtil;
import io.smallrye.asyncapi.runtime.generator.CustomDefinitionProvider;
import io.smallrye.asyncapi.runtime.io.channel.operation.OperationConstant;
import io.smallrye.asyncapi.runtime.io.components.ComponentReader;
import io.smallrye.asyncapi.runtime.io.info.InfoReader;
import io.smallrye.asyncapi.runtime.io.server.ServerReader;
import io.smallrye.asyncapi.runtime.util.JandexUtil;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans a deployment (using the archive and jandex annotation index) for relevant annotations. These
 * annotations, if found, are used to generate a valid AsyncAPI model.
 *
 * @author gasper.vrhovsek@gmail.com
 */
public class GidAnnotationScanner extends BaseAnnotationScanner {
    private static final Logger LOG = Logger.getLogger(GidAnnotationScanner.class);

    public static final DotName DOTNAME_EVENT_APP_DEFINITION = DotName.createSimple(EventApp.class.getName());

    private final SchemaGenerator schemaGenerator;

    /**
     * Constructor.
     *
     * @param config AsyncApiConfig instance
     * @param index IndexView of deployment
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
        Aai20Document messageHandlerAaiDocument = null;
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
        // Process @MessageHandler
        processMessageHandlerAnnotations(annotationScannerContext, asyncApi);
        processContextDefinitionReferencedSchemas(annotationScannerContext, asyncApi);

        return asyncApi;
    }

    private void processMessageHandlerAnnotations(AnnotationScannerContext context, Aai20Document asyncApi)
            throws ClassNotFoundException {
        DotName messageHandlerDotName = DotName.createSimple(MessageHandler.class.getName());
        DotName fanoutMessageHandlerDotName = DotName.createSimple(FanoutMessageHandler.class.getName());
        DotName topicMessageHandlerDotName = DotName.createSimple(TopicMessageHandler.class.getName());

        Stream<AnnotationInstance> directAnnotations = context.getIndex().getAnnotations(messageHandlerDotName)
                .stream().filter(this::annotatedMethods);
        Stream<AnnotationInstance> fanoutAnnotations = context.getIndex().getAnnotations(fanoutMessageHandlerDotName)
                .stream().filter(this::annotatedMethods);
        Stream<AnnotationInstance> topicAnnotations = context.getIndex().getAnnotations(topicMessageHandlerDotName)
                .stream().filter(this::annotatedMethods);

        Stream<AnnotationInstance> concat = Stream.concat(directAnnotations, fanoutAnnotations);
        List<AnnotationInstance> methodAnnos = Stream.concat(concat, topicAnnotations).collect(Collectors.toList());

        Map<String, ObjectNode> outgoingEvents = new HashMap<>();
        Map<String, ObjectNode> incomingEvents = new HashMap<>();
        Map<String, String> messageTypes = new HashMap<>();

        for (AnnotationInstance methodAnno : methodAnnos) {
            MethodInfo methodInfo = (MethodInfo) methodAnno.target();
            Type returnType = methodInfo.returnType();
            String type = JandexUtil.stringValue(methodAnno, "type");

            if (!returnType.kind().equals(Type.Kind.VOID)) {
                // Return type is not void, we can scan the class and add it to schemas
                String simpleName = loadClass(returnType.toString()).getSimpleName();
                ObjectNode node = generateJsonSchema(returnType.toString());
                outgoingEvents.put(simpleName, node);
                messageTypes.put(simpleName, type);
            }
            List<Type> parameters = methodInfo.parameters();
            Type eventType;
            if (parameters.size() != 1) {
                if (methodAnno.value("eventType") == null) {
                    throw new IllegalArgumentException(
                            "When multiple method parameters present eventType annotation property is required");
                }
                eventType = methodAnno.value("eventType").asClass();
            } else {
                eventType = parameters.get(0);
            }
            String simpleName = loadClass(eventType.toString()).getSimpleName();
            ObjectNode node = generateJsonSchema(eventType.toString());
            incomingEvents.put(simpleName, node);
            messageTypes.put(simpleName, type);
        }

        insertComponentSchemas(context, incomingEvents, asyncApi);
        insertComponentSchemas(context, outgoingEvents, asyncApi);

        // TODO handle fanout, topic, add bindings
        createChannels(incomingEvents, messageTypes, OperationConstant.PROP_SUBSCRIBE, asyncApi);
        createChannels(outgoingEvents, messageTypes, OperationConstant.PROP_PUBLISH, asyncApi);
    }

    private void processContextDefinitionReferencedSchemas(AnnotationScannerContext context, Aai20Document asyncApi) {
        Map<String, AaiSchema> definitionSchemaMap = context.getDefinitionSchemaMap();
        definitionSchemaMap.forEach((key, aaiSchema) -> {
            asyncApi.components.schemas.put(key, aaiSchema);
        });
        context.clearDefinitionSchemaMap();
    }

    private Aai20Document processEventAppDefinition(final AnnotationScannerContext context, Aai20Document document) {
        Collection<AnnotationInstance> annotations = context.getIndex()
                .getAnnotations(DOTNAME_EVENT_APP_DEFINITION);
        List<AnnotationInstance> packageDefs = annotations
                .stream()
                .filter(this::annotatedClasses)
                .collect(Collectors.toList());

        // Here we have packageDefs, now to build the AsyncAPI
        for (AnnotationInstance packageDef : packageDefs) {
            Aai20Document packageAai = new Aai20Document();
            packageAai.id = JandexUtil.stringValue(packageDef, PROP_ID);
            packageAai.info = InfoReader.readInfo(packageDef.value(PROP_INFO));
            packageAai.servers = ServerReader.readServers(packageDef.value(PROP_SERVERS)).orElse(null);
            packageAai.components = ComponentReader.create();
            MergeUtil.merge(document, packageAai);
        }
        return document;
    }

    private ObjectNode generateJsonSchema(String className) throws ClassNotFoundException {
        return schemaGenerator.generateSchema(loadClass(className));
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
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7,
                OptionPreset.PLAIN_JSON)
                        .with(Option.DEFINITIONS_FOR_ALL_OBJECTS);

        Set<String> ignorePackagePrefixes = config.convertExternalTypesToObjectIgnoredPackages();
        if (!ignorePackagePrefixes.isEmpty()) {
            configBuilder.forTypesInGeneral()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownTypeToObject(ignorePackagePrefixes));

            configBuilder.forFields()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownFieldToObject(ignorePackagePrefixes));
        }

        SchemaGeneratorConfig schemaGeneratorConfig = configBuilder.build();
        return new SchemaGenerator(schemaGeneratorConfig);
    }

}
