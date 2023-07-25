package org.iris_events.plugin.asyncapi.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.iris_events.asyncapi.runtime.client.ApicurioClient;

import io.apicurio.registry.rest.client.RegistryClientFactory;

@SuppressWarnings("unused")
@Mojo(name = "upload-schema", defaultPhase = LifecyclePhase.NONE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = false)
public class UploadSchemaMojo extends AbstractMojo {

    @Parameter(property = "schemaFilePath")
    private String schemaFilePath;

    @Parameter(defaultValue = "false", property = "skip")
    private boolean skip;

    @Parameter(property = "registryUrl")
    private String registryUrl;

    @Parameter(property = "artifactId")
    private String artifactId;

    @Parameter(property = "artifactVersion")
    private String artifactVersion;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping schema upload.");
            return;
        }
        try {
            getLog().info(String.format("Uploading schema file %s as artifactId = %s and version = %s to registry %s",
                    schemaFilePath, artifactId, artifactVersion, registryUrl));

            if (registryUrl != null) {
                Path path = Path.of(schemaFilePath);
                String asyncapiContent = Files.readString(path);
                ApicurioClient client = new ApicurioClient(RegistryClientFactory.create(registryUrl));
                client.upload(artifactId, artifactVersion, asyncapiContent);
            }
        } catch (IOException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("Could not generate OpenAPI Schema", ex);
        }
    }
}
