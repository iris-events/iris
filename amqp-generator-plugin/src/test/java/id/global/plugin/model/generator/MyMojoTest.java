package id.global.plugin.model.generator;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

import java.io.File;

import id.global.plugin.model.generator.models.ArtifactSource;

public class MyMojoTest extends AbstractMojoTestCase {

    private static final String ASYNC_API_FILENAME = "asyncapi.json";
    private static final String ASYNC_API_DIRECTORY = "src,test,resources";
    private static final String MODEL_NAME = "test-models";
    private static final ArtifactSource ARTIFACT_SOURCE = ArtifactSource.FILE;
    private static final String PACKAGE_NAME = "id.global.amqp.models";
    private static final String MODEL_VERSION = "2.0.0";
    private static final String URL = "https://schema.internal.globalid.dev";


    public void testPlugin() throws Exception {
        File pom = getTestFile("src/test/resources/pom-test.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        AmqpGeneratorMojo myMojo = (AmqpGeneratorMojo) super.lookupMojo("generate-amqp-models", "src/test/resources/pom-test.xml");
        assertNotNull(myMojo);


        setMojoProperties(myMojo, pom);
        myMojo.execute();
    }

    private MavenProject createMavenProject(File file) {
        Model model = new Model();
        model.setPomFile(file);

        return new MavenProject(model);
    }

    private void setMojoProperties(AmqpGeneratorMojo mojo, File pom) {
        mojo.project = createMavenProject(pom);
        mojo.asyncApiFilename = ASYNC_API_FILENAME;
        mojo.asyncApiDirectory = ASYNC_API_DIRECTORY;
        mojo.modelName = MODEL_NAME;
        mojo.artifactSource = ARTIFACT_SOURCE;
        mojo.packageName = PACKAGE_NAME;
        mojo.modelVersion = MODEL_VERSION;
        mojo.apicurioUrl = URL;
    }

}
