package org.iris_events.plugin.model.generator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class ExistingJavaTypeProcessorTest {
    private static final String ASYNCAPI_FILENAME = "src/test/resources/asyncapi.json";
    private static final String EXPECTED_ASYNCAPI = "src/test/resources/asyncapi-after-java-type-fix.json";

    @Test
    void fixExistingType() throws IOException, JSONException {
        var asyncapi = Paths.get(ASYNCAPI_FILENAME);
        var expected = Paths.get(EXPECTED_ASYNCAPI);

        String asyncapiContent = Files.readString(asyncapi);
        String expectedContent = Files.readString(expected);

        final var processor = new ExistingJavaTypeProcessor(new ObjectMapper());
        final var fixedContent = processor.fixExistingType(asyncapiContent);

        JSONAssert.assertEquals("Json contents should match", expectedContent, fixedContent, JSONCompareMode.NON_EXTENSIBLE);
    }
}
