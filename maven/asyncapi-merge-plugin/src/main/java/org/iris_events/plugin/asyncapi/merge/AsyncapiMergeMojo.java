package org.iris_events.plugin.asyncapi.merge;

import static org.iris_events.asyncapi.runtime.json.IrisObjectMapper.getObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.iris_events.asyncapi.runtime.client.ApicurioClient;
import org.iris_events.asyncapi.runtime.client.ClientDefinitionMerger;
import org.iris_events.asyncapi.runtime.client.ClientDefinitionParser;
import org.iris_events.asyncapi.runtime.scanner.model.ClientDefinitions;
import org.iris_events.asyncapi.runtime.util.VersionUtil;
import io.apicurio.registry.rest.client.RegistryClientFactory;

@Mojo(name = "generate-client-asyncapi", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false)
public class AsyncapiMergeMojo extends AbstractMojo {
    private static final String INITIAL_VERSION = "1.0.0";
    private static final String VERSION_PLACEHOLDER = "{VERSION}";

    @Parameter(property = "apicurioUrl")
    String apicurioUrl;

    @Parameter(property = "artifactGroup", defaultValue = "org.iris_events.events")
    String artifactGroup;

    @Parameter(property = "clientArtifactId", defaultValue = "globalid-client-api")
    String clientArtifactId;

    @Parameter(property = "versionIncrementSpillOver", defaultValue = "10")
    int versionSpillover;

    @Parameter(property = "skip", defaultValue = "false")
    boolean skip;

    public void execute() throws MojoExecutionException {
        Log log = getLog();

        if (skip) {
            log.info("Skipping asyncapi client definitions merge.");
            return;
        }

        ApicurioClient client = new ApicurioClient(RegistryClientFactory.create(apicurioUrl));
        List<ClientDefinitions> definitionsList = getClientDefinitions(client, artifactGroup);
        ClientDefinitionMerger merger = new ClientDefinitionMerger();

        String version = INITIAL_VERSION;
        try {
            JsonNode merged = merger.merge(definitionsList, version);

            String latestClientArtifact = client.getLatestClientArtifact(clientArtifactId);
            if (latestClientArtifact != null) {
                JsonNode upstreamClientDefinition = getObjectMapper().readTree(latestClientArtifact);

                if (isClientSchemaUpToDate(merged, upstreamClientDefinition)) {
                    log.info("Client schema already up to date, not uploading new version");
                    return;
                }
                version = VersionUtil.bumpVersion(getVersion(upstreamClientDefinition), versionSpillover);
            }
            client.uploadClientSchema(clientArtifactId, null, getVersionedSchema(merged, version));
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }

        log.info("Successfully uploaded new version of merged client definitions");
    }

    private String getVersionedSchema(JsonNode schema, String version) throws JsonProcessingException {
        return getObjectMapper().writeValueAsString(schema).replace(VERSION_PLACEHOLDER, version);
    }

    private String getVersion(JsonNode upstreamClientDefinition) {
        return upstreamClientDefinition.findValue("version").textValue();
    }

    private boolean isClientSchemaUpToDate(JsonNode clientDefinition, JsonNode upstreamClientDefinition)
            throws JsonProcessingException {
        final var upstreamVersion = getVersion(upstreamClientDefinition);
        final var versionedSchema = getVersionedSchema(clientDefinition, upstreamVersion);
        final var versionedSchemaNode = getObjectMapper().readTree(versionedSchema);
        return upstreamClientDefinition.equals(versionedSchemaNode);
    }

    private List<ClientDefinitions> getClientDefinitions(ApicurioClient client, String artifactGroup) {
        ClientDefinitionParser parser = new ClientDefinitionParser();
        return client.getArtifactsInGroup(artifactGroup).stream().map(artifact -> {
            try {
                return parser.parse(getObjectMapper().readTree(artifact));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
