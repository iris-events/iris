package org.iris_events.plugin.model.generator;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import org.iris_events.plugin.model.generator.models.ArtifactSource;

import jakarta.annotation.Nullable;

public class AmqpGeneratorPluginTest extends AbstractMojoTestCase {

    private static final String ASYNC_API_FILENAME = "asyncapi.json";
    private static final String ASYNC_API_DIRECTORY = "src,test,resources";
    private static final String MODEL_NAME = "test-models";
    private static final ArtifactSource ARTIFACT_SOURCE = ArtifactSource.FILE;
    private static final String PACKAGE_NAME = "id.global.iris.models";
    private static final String MODEL_VERSION = "2.0.0";
    private static final String URL = "https://schema.internal.globalid.dev";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testPlugin() throws Exception {
        File pom = getTestFile("src/test/resources/pom-test.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        AmqpGeneratorMojo myMojo = (AmqpGeneratorMojo) super.lookupMojo("generate-amqp-models",
                "src/test/resources/pom-test.xml");
        assertNotNull(myMojo);

        setMojoProperties(myMojo, pom);
        myMojo.execute();
    }

    private void setMojoProperties(@Nullable AmqpGeneratorMojo mojo, File pom) {
        mojo.asyncApiFilename = ASYNC_API_FILENAME;
        mojo.asyncApiDirectory = ASYNC_API_DIRECTORY;
        mojo.modelName = MODEL_NAME;
        mojo.artifactSource = ARTIFACT_SOURCE;
        mojo.packageName = PACKAGE_NAME;
        mojo.modelVersion = MODEL_VERSION;
        mojo.apicurioUrl = URL;
    }

}
