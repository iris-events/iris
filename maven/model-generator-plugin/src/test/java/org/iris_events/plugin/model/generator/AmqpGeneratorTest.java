package org.iris_events.plugin.model.generator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.iris_events.plugin.model.generator.exception.AmqpGeneratorException;
import org.iris_events.plugin.model.generator.models.ArtifactSource;
import org.iris_events.plugin.model.generator.utils.CustomDependencies;
import org.iris_events.plugin.model.generator.utils.FileInteractor;
import org.iris_events.plugin.model.generator.utils.PathResolver;
import org.iris_events.plugin.model.generator.utils.SchemaFileGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

class AmqpGeneratorTest {

    private static final String PACKAGE_NAME = "org.iris_events.amqp.test";
    private static final String MODEL_VERSION = "0.1";
    private static final String MODEL_NAME = "amqpGeneratorTest";
    private static final String ASYNCAPI_FILENAME = "asyncapi.json";
    private static final String TEST_RESOURCES_DIR = "src/test/resources";
    private static final String APICURIO_URL = "";

    private static final LinkOption[] LINK_OPTIONS = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };

    @SuppressWarnings("unused")
    private static Stream<Arguments> generate() {
        final var testModelPath = "models/src/main/java/org/iris_events/amqp/test/amqpgeneratortest";
        return Stream.of(
                Arguments.of(new GenerateArguments("models", true)),
                Arguments.of(new GenerateArguments("models/src/main/java", true)),
                Arguments.of(new GenerateArguments(testModelPath, true)),
                Arguments.of(new GenerateArguments(testModelPath + "/CachedEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/EventDefaults.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/FanoutTestEventV1.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/FrontendTestEventV1.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/GeneratedTestEvent.java", false)),
                Arguments.of(new GenerateArguments(testModelPath + "/ListPayloadEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/MapPayloadEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/PassthroughInboundEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/PassthroughOutboundEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/ProducedEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/RpcRequestEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/RpcResponseEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/TestEventV1.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/TestEventV2.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/TopicTestEventV1.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/TestEventWithRequirements.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/TestEventWithDocumentation.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/MapPayloadEvent.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/payload", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/payload/Status.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/payload/User.java", true)),
                Arguments.of(new GenerateArguments(testModelPath + "/payload/MapValue.java", true)),
                Arguments.of(new GenerateArguments("models/pom.xml", true)));
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

        Log log = new SystemStreamLog();

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
                APICURIO_URL,
                new CustomDependencies(""));

        generator.generate(ArtifactSource.FILE);
    }

    @ParameterizedTest
    @MethodSource
    public void generate(GenerateArguments arg) throws IOException {
        Path generatedModelPath = Paths.get(arg.path);

        final var shouldGenerate = arg.shouldGenerate();
        assertThat(Files.exists(generatedModelPath, LINK_OPTIONS), is(shouldGenerate));
        if (!shouldGenerate) {
            return;
        }
        Path expectedModelPath = Paths.get(TEST_RESOURCES_DIR + "/" + arg.path);
        assertThat(Files.exists(expectedModelPath, LINK_OPTIONS), is(true));

        if (arg.path.endsWith(".java")) {
            if (Files.mismatch(generatedModelPath, expectedModelPath) != -1L) {
                showDiff(generatedModelPath, expectedModelPath);
            }
        }
    }

    private void showDiff(Path generatedFilePath, Path expectedFilePath) throws IOException {
        var generated = Files.readAllLines(generatedFilePath).stream().map(String::stripTrailing)
                .collect(Collectors.joining("\n"));
        var expected = Files.readAllLines(expectedFilePath).stream().map(String::stripTrailing)
                .collect(Collectors.joining("\n"));

        Assertions.assertEquals(expected, generated);
    }

    private record GenerateArguments(String path, boolean shouldGenerate) {
    }
}
