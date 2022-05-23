package id.global.iris.asyncapi.runtime.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.iris.asyncapi.runtime.json.EdaObjectMapper;
import id.global.iris.asyncapi.runtime.scanner.model.ClientDefinitions;

class ClientDefinitionMergerTest {

    private static final String MERGE_FILE_1 = "src/test/resources/asyncapi_merge_1.json";
    private static final String MERGE_FILE_2 = "src/test/resources/asyncapi_merge_2.json";
    private static final String MERGE_FILE_3 = "src/test/resources/asyncapi_merge_3.json";
    private static final String MERGED_FILE = "src/test/resources/asyncapi_merged.json";

    @Test
    void merge() throws IOException, JSONException {
        File file1 = new File(MERGE_FILE_1);
        File file2 = new File(MERGE_FILE_2);
        File file3 = new File(MERGE_FILE_3);
        File merged = new File(MERGED_FILE);

        ObjectMapper objectMapper = EdaObjectMapper.getObjectMapper();
        JsonNode rootNode1 = objectMapper.readTree(file1);
        JsonNode rootNode2 = objectMapper.readTree(file2);
        JsonNode rootNode3 = objectMapper.readTree(file3);
        JsonNode mergedNode = objectMapper.readTree(merged);

        ClientDefinitionParser parser = new ClientDefinitionParser();
        ClientDefinitions definitions1 = parser.parse(rootNode1);
        ClientDefinitions definitions2 = parser.parse(rootNode2);
        ClientDefinitions definitions3 = parser.parse(rootNode3);

        List<ClientDefinitions> definitionsList = List.of(definitions1, definitions2, definitions3);

        ClientDefinitionMerger merger = new ClientDefinitionMerger();
        JsonNode mergedDefinitions = merger.merge(definitionsList, "1.0.0");

        JSONAssert.assertEquals(objectMapper.writeValueAsString(mergedNode), objectMapper.writeValueAsString(mergedDefinitions),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
