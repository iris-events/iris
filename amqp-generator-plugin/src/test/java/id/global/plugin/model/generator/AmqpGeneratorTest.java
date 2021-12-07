package id.global.plugin.model.generator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.plugin.model.generator.exception.AmqpGeneratorException;
import id.global.plugin.model.generator.models.ArtifactSource;
import id.global.plugin.model.generator.utils.FileInteractor;
import id.global.plugin.model.generator.utils.PathResolver;
import id.global.plugin.model.generator.utils.SchemaFileGenerator;

class AmqpGeneratorTest {

    private static final String PACKAGE_NAME = "id.global.amqp.test";
    private static final String MODEL_VERSION = "0.1";
    private static final String MODEL_NAME = "amqpGeneratorTest";
    private static final String ASYNCAPI_FILENAME = "asyncapi.json";
    private static final String TEST_RESOURCES_DIR = "src/test/resources";
    private static final String APICURIO_URL = "";

    private static final LinkOption[] LINK_OPTIONS = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };

    @BeforeEach
    void setUp() throws IOException, AmqpGeneratorException {
        Path models = Paths.get("models");
        if (Files.exists(models)) {
            Files.walk(models)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        Log log = new SilentLog();

        PathResolver pathResolver = new PathResolver();
        ObjectMapper objectMapper = new ObjectMapper();
        FileInteractor fileInteractor = new FileInteractor(pathResolver);

        SchemaFileGenerator schemaFileGenerator = new SchemaFileGenerator(log, pathResolver, fileInteractor,
                objectMapper);

        AmqpGenerator generator = new AmqpGenerator(
                schemaFileGenerator,
                objectMapper,
                pathResolver,
                fileInteractor,
                log,
                PACKAGE_NAME,
                MODEL_VERSION,
                MODEL_NAME,
                ASYNCAPI_FILENAME,
                TEST_RESOURCES_DIR,
                APICURIO_URL
        );

        generator.generate(ArtifactSource.FILE);

    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> generate() {
        return Stream.of(
                Arguments.of(new GenerateArguments("models")),
                Arguments.of(new GenerateArguments("models/src/main/java")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/EventDefaults.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/FanoutTestEventV1.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/FrontendTestEventV1.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/GeneratedTestEvent.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/PassthroughInboundEvent.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/PassthroughOutboundEvent.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/ProducedEvent.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/TestEventV1.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/TestEventV2.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpGeneratorTest/TopicTestEventV1.java")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/payload")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/payload/Status.java")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpGeneratorTest/payload/User.java")),
                Arguments.of(new GenerateArguments("models/pom.xml"))

        );
    }

    @ParameterizedTest
    @MethodSource
    public void generate(GenerateArguments arg) throws IOException {
        Path generatedModelPath = Paths.get(arg.path);
        Path expectedModelPath = Paths.get(TEST_RESOURCES_DIR + "/" + arg.path);

        assertThat(Files.exists(generatedModelPath, LINK_OPTIONS), is(true));
        assertThat(Files.exists(expectedModelPath, LINK_OPTIONS), is(true));

        if (arg.path.endsWith(".java")) {
            if (Files.mismatch(generatedModelPath, expectedModelPath) != -1L) {
                showDiff(generatedModelPath, expectedModelPath);
            }
        }

    }

    private void showDiff(Path generatedFilePath, Path expectedFilePath) throws IOException {
        Stream<String> generatedFileLines = Files.lines(generatedFilePath);
        Stream<String> expectedFileLines = Files.lines(expectedFilePath);

        Iterator<String> generatedIt = generatedFileLines.iterator();
        Iterator<String> expectedIt = expectedFileLines.iterator();

        int lineNumber = 1;

        while (generatedIt.hasNext() && expectedIt.hasNext()) {
            String lineGenerated = generatedIt.next();
            String lineExpected = expectedIt.next();
            if (!lineGenerated.equals(lineExpected)) {
                fail(String.format("Files differ on line %d: Generated file line:\n%s\nExpected file line:\n%s", lineNumber,
                        lineGenerated, lineExpected));
            }
            lineNumber++;
        }
        if (generatedIt.hasNext() || expectedIt.hasNext()) {
            fail(String.format("Files %s and %s differ in length", generatedFilePath, expectedFilePath));
        }
    }

    private record GenerateArguments(String path) {
    }
}