package io.smallrye.asyncapi.runtime.scanner;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.smallrye.asyncapi.runtime.io.JsonUtil;
import io.smallrye.asyncapi.runtime.io.schema.SchemaReader;

public class SchemaReaderTest extends IndexScannerTestBase {

    @Test
    public void readJsonSchemaToAPISchema() throws IOException {

        String schemaString = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"Status\":{\"type\":\"string\",\"enum\":[\"inProgress\",\"actionRequired\",\"completed\",\"declined\",\"expired\"]}},\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"},\"clientId\":{\"type\":\"string\"},\"actionId\":{\"type\":\"string\"},\"uuid\":{\"type\":\"string\"},\"createdAt\":{\"type\":\"string\"},\"status\":{\"$ref\":\"#/definitions/Status\"}}}";
        String propertiesString = "{\"id\":{\"type\":\"string\"},\"clientId\":{\"type\":\"string\"},\"actionId\":{\"type\":\"string\"},\"uid\":{\"type\":\"string\"},\"createdAt\":{\"type\":\"string\"},\"status\":{\"$ref\":\"#/definitions/Status\"}}";

        ObjectMapper mapper = JsonUtil.MAPPER;
        JsonNode schemaNode = mapper.readTree(schemaString);
        JsonNode propertiesNode = mapper.readTree(propertiesString);

        Optional<Map<String, AaiSchema>> propertiesOpt = SchemaReader.readSchemas(propertiesNode);

        assertTrue(propertiesOpt.isPresent());
        Map<String, AaiSchema> stringAaiSchemaMap = propertiesOpt.get();
        Assert.assertEquals(6, stringAaiSchemaMap.size());

        AaiSchema aaiSchema = SchemaReader.readSchema(schemaNode);
        Map<String, AaiSchema> properties = aaiSchema.properties;
        Assert.assertEquals(6, properties.size());
    }
}
