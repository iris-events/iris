package id.global.iris.asyncapi.runtime.scanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jboss.jandex.Index;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import id.global.iris.asyncapi.runtime.json.IrisObjectMapper;
import id.global.iris.asyncapi.runtime.scanner.app.EventHandlersApp;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.annotations.SnapshotMessageHandler;
import id.global.iris.common.message.SnapshotRequested;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;

public class GidAnnotationScannerTest extends IndexScannerTestBase {
    private static final String EXPECTED_JSON_RESULT_FILE = "src/test/resources/asyncapi.json";

    @Test
    @DisplayName("Generate JSON from a test app and compare to expected result.")
    public void assertGeneratedJson() throws IOException, JSONException {
        File file = new File(EXPECTED_JSON_RESULT_FILE);
        String expectedContent = Files.readString(file.toPath());

        var projectName = EventHandlersApp.class.getSimpleName();
        var projectGroupId = EventHandlersApp.class.getCanonicalName().replaceAll("." + projectName, "");
        var projectVersion = "1.0.0";
        Index index = getEventHandlersAppIndex();
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index, projectName, projectGroupId,
                projectVersion);
        Aai20Document document = scanner.scan();
        String schemaString = IrisObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(document);
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
                SnapshotRequested.class,
                MessageHandler.class,
                SnapshotMessageHandler.class,
                Message.class);
    }
}
