package id.global.iris.plugin.model.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.global.iris.plugin.model.generator.exception.AmqpGeneratorException;
import id.global.iris.plugin.model.generator.models.ArtifactSource;
import id.global.iris.plugin.model.generator.utils.FileInteractor;
import id.global.iris.plugin.model.generator.utils.PathResolver;
import id.global.iris.plugin.model.generator.utils.SchemaFileGenerator;
import junit.framework.Assert;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AmqpGeneratorTest {

    private static final String PACKAGE_NAME = "id.global.amqp.test";
    private static final String MODEL_VERSION = "0.1";
    private static final String MODEL_NAME = "amqpGeneratorTest";
    private static final String ASYNCAPI_FILENAME = "asyncapi.json";
    private static final String TEST_RESOURCES_DIR = "src/test/resources";
    private static final String APICURIO_URL = "";

    private static final LinkOption[] LINK_OPTIONS = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };

    @SuppressWarnings("unused")
    private static Stream<Arguments> generate() {
        return Stream.of(
                Arguments.of(new GenerateArguments("models")),
                Arguments.of(new GenerateArguments("models/src/main/java")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/EventDefaults.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/FanoutTestEventV1.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/FrontendTestEventV1.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/GeneratedTestEvent.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/PassthroughInboundEvent.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/PassthroughOutboundEvent.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/ProducedEvent.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/TestEventV1.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/TestEventV2.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/TopicTestEventV1.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/MapPayloadEvent.java")),
                Arguments.of(new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/payload")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/payload/Status.java")),
                Arguments.of(
                        new GenerateArguments("models/src/main/java/id/global/amqp/test/amqpgeneratortest/payload/User.java")),
                Arguments.of(new GenerateArguments(
                        "models/src/main/java/id/global/amqp/test/amqpgeneratortest/payload/MapValue.java")),
                Arguments.of(new GenerateArguments("models/pom.xml"))

        );
    }

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
                APICURIO_URL);

        generator.generate(ArtifactSource.FILE);

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

    private void showDiffOld(Path generatedFilePath, Path expectedFilePath) throws IOException {
        Stream<String> generatedFileLines = Files.lines(generatedFilePath);
        Stream<String> expectedFileLines = Files.lines(expectedFilePath);

        Iterator<String> generatedIt = generatedFileLines.iterator();
        Iterator<String> expectedIt = expectedFileLines.iterator();

        int lineNumber = 1;
        while (generatedIt.hasNext() && expectedIt.hasNext()) {
            String lineGenerated = generatedIt.next();
            String lineExpected = expectedIt.next();
            Assert.assertEquals("Files differ on line " + lineNumber, lineExpected, lineGenerated);
            /*
             * if (!lineGenerated.equals(lineExpected)) {
             * fail(String.format("Files differ on line %d: Generated file line:\n%s\nExpected file line:\n%s", lineNumber,
             * lineGenerated, lineExpected));
             * }
             */
            lineNumber++;
        }
        if (generatedIt.hasNext() || expectedIt.hasNext()) {
            fail(String.format("Files %s and %s differ in length", generatedFilePath, expectedFilePath));
        }
    }

    private void showDiff(Path generatedFilePath, Path expectedFilePath) throws IOException {
        var generated = Files.readAllLines(generatedFilePath).stream().map(String::stripTrailing)
                .collect(Collectors.joining("\n"));
        var expected = Files.readAllLines(expectedFilePath).stream().map(String::stripTrailing)
                .collect(Collectors.joining("\n"));

        Assertions.assertEquals(expected, generated);
    }

    private record GenerateArguments(String path) {
    }
}
