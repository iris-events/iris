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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiMessage;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.api.util.MergeUtil;
import io.smallrye.asyncapi.runtime.generator.CustomDefinitionProvider;
import io.smallrye.asyncapi.runtime.io.JsonUtil;
import io.smallrye.asyncapi.runtime.io.channel.ChannelConstant;
import io.smallrye.asyncapi.runtime.io.channel.ChannelReader;
import io.smallrye.asyncapi.runtime.io.components.ComponentReader;
import io.smallrye.asyncapi.runtime.io.info.InfoReader;
import io.smallrye.asyncapi.runtime.io.message.ApiMessageConstant;
import io.smallrye.asyncapi.runtime.io.message.MessageReader;
import io.smallrye.asyncapi.runtime.io.schema.SchemaConstant;
import io.smallrye.asyncapi.runtime.io.server.ServerReader;
import io.smallrye.asyncapi.runtime.scanner.model.JsonSchemaInfo;
import io.smallrye.asyncapi.runtime.util.JandexUtil;
import io.smallrye.asyncapi.spec.annotations.AsyncAPIDefinition;
import io.smallrye.asyncapi.spec.annotations.EventApp;

/**
 * Scans a deployment (using the archive and jandex annotation index) for relevant annotations. These
 * annotations, if found, are used to generate a valid AsyncAPI model.
 *
 * @author eric.wittmann@gmail.com
 */
public class AsyncApiAnnotationScanner extends BaseAnnotationScanner {
    private static final Logger LOG = Logger.getLogger(AsyncApiAnnotationScanner.class);

    public static final DotName DOTNAME_ASYNC_API_DEFINITION = DotName.createSimple(AsyncAPIDefinition.class.getName());
    public static final DotName DOTNAME_EVENT_APP_DEFINITION = DotName.createSimple(EventApp.class.getName());

    public static final String PROP_ID = "id";
    public static final String PROP_INFO = "info";
    public static final String PROP_SERVERS = "servers";
    public static final String PROP_CHANNELS = "channels";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";
    public static final String PROP_MESSAGE_TYPE = "type";

    private final SchemaGenerator schemaGenerator;

    /**
     * Constructor.
     *
     * @param config AsyncApiConfig instance
     * @param index IndexView of deployment
     */
    public AsyncApiAnnotationScanner(AsyncApiConfig config, IndexView index) {
        super(config, index);
        // Schema generator JsonSchema of components
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7,
                OptionPreset.PLAIN_JSON)
                        .with(Option.DEFINITIONS_FOR_ALL_OBJECTS);

        Set<String> excludeFromSchemas = config.excludeFromSchemas();
        if (!excludeFromSchemas.isEmpty()) {
            configBuilder.forTypesInGeneral()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownTypeToObject(excludeFromSchemas));

            configBuilder.forFields()
                    .withCustomDefinitionProvider(CustomDefinitionProvider.convertUnknownFieldToObject(excludeFromSchemas));
        }

        SchemaGeneratorConfig schemaGeneratorConfig = configBuilder.build();
        schemaGenerator = new SchemaGenerator(schemaGeneratorConfig);
    }

    /**
     * Scan the deployment for relevant annotations. Returns an AsyncAPI data model that was
     * built from those found annotations.
     *
     * @return Document generated from scanning annotations
     */
    public Aai20Document scan() {
        LOG.debug("Scanning deployment for Async Annotations.");
        return scanMicroProfileAsyncApiAnnotations();
    }

    private Aai20Document scanMicroProfileAsyncApiAnnotations() {
        Aai20Document asyncApi = this.annotationScannerContext.getAsyncApi();

        // Find all OpenAPIDefinition annotations at the package level
        processPackageAsyncAPIDefinitions(annotationScannerContext, asyncApi);

        processClassSchemas(annotationScannerContext, asyncApi);

        processContextDefinitionReferencedSchemas(annotationScannerContext, asyncApi);

        processClassMessageItems(annotationScannerContext, asyncApi);

        processClassChannelItems(annotationScannerContext, asyncApi);

        return asyncApi;
    }

    private void processContextDefinitionReferencedSchemas(AnnotationScannerContext context, Aai20Document asyncApi) {
        Map<String, AaiSchema> definitionSchemaMap = context.getDefinitionSchemaMap();
        definitionSchemaMap.forEach((key, aaiSchema) -> {
            asyncApi.components.schemas.put(key, aaiSchema);
        });
        context.clearDefinitionSchemaMap();
    }

    private Aai20Document processPackageAsyncAPIDefinitions(final AnnotationScannerContext context,
            Aai20Document asyncApi) {

        Collection<AnnotationInstance> annotations = context.getIndex()
                .getAnnotations(DOTNAME_ASYNC_API_DEFINITION);
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
            packageAai.channels = ChannelReader.readChannels(packageDef.value(PROP_CHANNELS)).orElse(null);
            packageAai.components = ComponentReader.create();

            MergeUtil.merge(asyncApi, packageAai);
        }
        return asyncApi;
    }

    private Aai20Document processClassMessageItems(AnnotationScannerContext context, Aai20Document asyncApi) {
        List<AnnotationInstance> messages = context.getIndex()
                .getAnnotations(ApiMessageConstant.DOTNAME_MESSAGE)
                .stream()
                .filter(this::annotatedClasses)
                .collect(Collectors.toList());

        messages.forEach(message -> {
            AaiMessage messageItem = MessageReader.readMessage(message);
            //            context.addMessageDefinition() // TODO
        });

        return null;
    }

    private Aai20Document processClassChannelItems(AnnotationScannerContext context, Aai20Document asyncApi) {
        List<AnnotationInstance> channels = context.getIndex()
                .getAnnotations(ChannelConstant.DOTNAME_CHANNEL)
                .stream()
                .filter(this::annotatedClasses)
                .collect(Collectors.toList());

        channels.forEach(channel -> {
            AaiChannelItem channelItem = ChannelReader.readChannel(channel);
            // TODO merge, in V1 we'll support only class annotated channels
            asyncApi.channels.put(channelItem.getName(), channelItem);
        });
        return asyncApi;
    }

    private void processClassSchemas(final AnnotationScannerContext context, Aai20Document aaiDocument) {
        ObjectMapper mapper = JsonUtil.MAPPER;

        Map<String, JsonSchemaInfo> collect = context.getIndex()
                .getAnnotations(SchemaConstant.DOTNAME_SCHEMA)
                .stream()
                .filter(this::annotatedClasses)
                .collect(Collectors.toMap(
                        annotationInstance -> annotationInstance.target().asClass().simpleName(),
                        annotationInstance -> {
                            try {
                                String className = annotationInstance.target().asClass().name().toString();
                                return new JsonSchemaInfo(annotationInstance.name(), className, generateJsonSchema(className),
                                        annotationInstance.values());
                            } catch (ClassNotFoundException e) {
                                LOG.error("Could not process class schemas.", e);
                                return new JsonSchemaInfo(null, null, null, null);
                            }
                        }));

        insertComponentSchemas(context, collect, aaiDocument);
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
}
