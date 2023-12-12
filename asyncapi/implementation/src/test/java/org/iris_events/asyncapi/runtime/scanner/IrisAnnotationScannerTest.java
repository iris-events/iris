package org.iris_events.asyncapi.runtime.scanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.annotation.security.RolesAllowed;

import org.iris_events.annotations.CachedMessage;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;
import org.iris_events.asyncapi.runtime.scanner.app.EventHandlersApp;
import org.iris_events.asyncapi.runtime.scanner.app.EventHandlersAppWithMapProperty;
import org.iris_events.asyncapi.runtime.scanner.app.ParseErrorEventHandlersApp;
import org.iris_events.common.message.SnapshotRequested;
import org.jboss.jandex.Index;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;

public class IrisAnnotationScannerTest extends IndexScannerTestBase {
    private static final String EXPECTED_JSON_RESULT_FILE = "src/test/resources/asyncapi.json";
    private static final String EXPECTED_JSON_MAP_VALUE_RESULT_FILE = "src/test/resources/asyncapi_map_value.json";
    private static final String EXPECTED_JSON_ALL_OF_VALUE_RESULT_FILE = "src/test/resources/asyncapi_allOf_value.json";

    @Test
    @DisplayName("Generate JSON from a test app and compare to expected result.")
    public void assertGeneratedJson() throws IOException, JSONException {
        File file = new File(EXPECTED_JSON_RESULT_FILE);
        String expectedContent = Files.readString(file.toPath());

        var projectName = EventHandlersApp.class.getSimpleName();
        var projectGroupId = EventHandlersApp.class.getCanonicalName().replaceAll("." + projectName, "");
        var projectVersion = "1.0.0";
        Index index = getEventHandlersAppIndex();
        IrisAnnotationScanner scanner = new IrisAnnotationScanner(emptyConfig(), index, projectName, projectGroupId,
                projectVersion, IrisObjectMapper.getObjectMapper());
        AsyncApi26Document document = scanner.scan();

        final var documentJsonNode = Library.writeDocument(document);
        final var schemaString = IrisObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(documentJsonNode);
        System.out.println("Generated schema:\n" + schemaString);
        JSONAssert.assertEquals("Json contents should match", expectedContent, schemaString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Generate JSON schema of event with object description resulting in allOf property")
    public void generateSchemaWithReferencedPropertyDescription() throws IOException, JSONException {
        File file = new File(EXPECTED_JSON_ALL_OF_VALUE_RESULT_FILE);
        String expectedContent = Files.readString(file.toPath());

        var projectName = ParseErrorEventHandlersApp.class.getSimpleName();
        var projectGroupId = ParseErrorEventHandlersApp.class.getCanonicalName().replaceAll("." + projectName, "");
        var projectVersion = "1.0.0";
        Index index = indexOf(ParseErrorEventHandlersApp.class,
                ParseErrorEventHandlersApp.EventWithDescribedEnum.class,
                MessageHandler.class,
                Message.class,
                RolesAllowed.class);

        IrisAnnotationScanner scanner = new IrisAnnotationScanner(emptyConfig(), index, projectName, projectGroupId,
                projectVersion, IrisObjectMapper.getObjectMapper());
        AsyncApi26Document document = scanner.scan();

        final var documentJsonNode = Library.writeDocument(document);
        final var schemaString = IrisObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(documentJsonNode);
        JSONAssert.assertEquals("Json contents should match", expectedContent, schemaString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Generate JSON schema of event with map value.")
    public void generateSchemaWithMappedValue() throws IOException, JSONException {
        File file = new File(EXPECTED_JSON_MAP_VALUE_RESULT_FILE);
        String expectedContent = Files.readString(file.toPath());

        var projectName = EventHandlersAppWithMapProperty.class.getSimpleName();
        var projectGroupId = EventHandlersAppWithMapProperty.class.getCanonicalName().replaceAll("." + projectName, "");
        var projectVersion = "1.0.0";
        Index index = indexOf(EventHandlersAppWithMapProperty.class,
                EventHandlersAppWithMapProperty.EventWithMapValue.class,
                MessageHandler.class,
                Message.class, RolesAllowed.class);

        IrisAnnotationScanner scanner = new IrisAnnotationScanner(emptyConfig(), index, projectName, projectGroupId,
                projectVersion, IrisObjectMapper.getObjectMapper());
        AsyncApi26Document document = scanner.scan();
        final var documentJsonNode = Library.writeDocument(document);
        final var schemaString = IrisObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(documentJsonNode);
        JSONAssert.assertEquals("Json contents should match", expectedContent, schemaString, JSONCompareMode.NON_EXTENSIBLE);
    }

    private static Index getEventHandlersAppIndex() {
        return indexOf(EventHandlersApp.class,
                EventHandlersApp.TestEventWithDocumentation.class,
                EventHandlersApp.TestEventWithRequirements.class,
                EventHandlersApp.TestEventV1.class,
                EventHandlersApp.TestEventV2.class,
                EventHandlersApp.FrontendTestEventV1.class,
                EventHandlersApp.TopicTestEventV1.class,
                EventHandlersApp.FanoutTestEventV1.class,
                EventHandlersApp.GeneratedTestEvent.class,
                EventHandlersApp.EventDefaults.class,
                EventHandlersApp.ProducedEvent.class,
                EventHandlersApp.PassthroughInboundEvent.class,
                EventHandlersApp.PassthroughOutboundEvent.class,
                EventHandlersApp.MapPayloadEvent.class,
                EventHandlersApp.ListPayloadEvent.class,
                EventHandlersApp.MapValue.class,
                EventHandlersApp.CachedEvent.class,
                EventHandlersApp.RpcRequestEvent.class,
                EventHandlersApp.RpcResponseEvent.class,
                SnapshotRequested.class,
                MessageHandler.class,
                SnapshotMessageHandler.class,
                Message.class,
                RolesAllowed.class,
                CachedMessage.class);
    }
}
