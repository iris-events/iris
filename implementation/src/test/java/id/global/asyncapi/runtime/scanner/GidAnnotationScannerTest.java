package id.global.asyncapi.runtime.scanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jboss.jandex.Index;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.asyncapi.runtime.io.MixinResolver;
import id.global.asyncapi.runtime.scanner.app.EventHandlersApp;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;

public class GidAnnotationScannerTest extends IndexScannerTestBase {
    private static final String EXPECTED_JSON_RESULT_FILE = "src/test/resources/asyncapi.json";

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setMixInResolver(new MixinResolver())
            .setVisibility(new ObjectMapper().getSerializationConfig().getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

    @Test
    @DisplayName("Generate JSON from a test app and compare to expected result.")
    public void assertGeneratedJson() throws IOException, JSONException {
        File file = new File(EXPECTED_JSON_RESULT_FILE);
        String expectedContent = Files.readString(file.toPath());

        var projectName = EventHandlersApp.class.getSimpleName();
        Index index = getEventHandlersAppIndex();
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index, projectName);
        Aai20Document document = scanner.scan();
        String schemaString = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        JSONAssert.assertEquals(schemaString, expectedContent, false);
    }

    private static Index getEventHandlersAppIndex() {
        return indexOf(EventHandlersApp.class,
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
                MessageHandler.class,
                Message.class);
    }
}
