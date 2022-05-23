package id.global.iris.plugin.model.generator.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

class ExistingJavaTypeProcessorTest {
    private static final String ASYNCAPI_FILENAME = "src/test/resources/asyncapi.json";
    private static final String EXPECTED_ASYNCAPI = "src/test/resources/asyncapi-after-java-type-fix.json";

    @Test
    void fixExistingType() throws IOException, JSONException {
        File asyncapi = new File(ASYNCAPI_FILENAME);
        File expected = new File(EXPECTED_ASYNCAPI);

        String asyncapiContent = Files.readString(asyncapi.toPath());
        String expectedContent = Files.readString(expected.toPath());

        final var processor = new ExistingJavaTypeProcessor();
        final var fixedContent = processor.fixExistingType(asyncapiContent);

        JSONAssert.assertEquals("Json contents should match", expectedContent, fixedContent, JSONCompareMode.NON_EXTENSIBLE);
    }
}