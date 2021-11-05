package id.global.plugin.model.generator.utils;

import static id.global.plugin.model.generator.utils.StringConstants.COMPONENTS_SCHEMAS;
import static id.global.plugin.model.generator.utils.StringConstants.EMPTY_STRING;
import static id.global.plugin.model.generator.utils.StringConstants.HASH;
import static id.global.plugin.model.generator.utils.StringConstants.PAYLOAD;

import java.nio.file.Path;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SchemaFileGenerator {
    private final Log log;
    private final PathResolver pathResolver;
    private final FileInteractor fileInteractor;
    private final ObjectMapper objectMapper;

    public SchemaFileGenerator(Log log, PathResolver pathResolver,
            FileInteractor fileInteractor, ObjectMapper objectMapper) {
        this.log = log;
        this.pathResolver = pathResolver;
        this.fileInteractor = fileInteractor;
        this.objectMapper = objectMapper;
    }

    public void createSchemaFiles(final JsonNode schemas, Set<String> eventClassNames) {
        log.info("Creating JsonSchema files for model generator!");
        schemas.fields()
                .forEachRemaining((schema) -> {
                    var schemaName = schema.getKey();
                    var schemaContent = "";
                    try {
                        schemaContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema.getValue());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error processing schema value", e);
                    }

                    Path schemaPath;
                    if (eventClassNames.contains(schemaName)) {
                        schemaPath = getEventSchemaPath(schemaName);
                    } else {
                        schemaPath = getPayloadSchemaPath(schemaName);
                    }

                    replaceAndSaveSchemaFiles(schemaContent, schemaPath);
                });
    }

    private Path getPayloadSchemaPath(final String fileName) {
        return pathResolver.getSchemasDirectory().resolve(PAYLOAD).resolve(fileName);
    }

    private Path getEventSchemaPath(final String fileName) {
        return pathResolver.getSchemasDirectory().resolve(fileName);
    }

    private void replaceAndSaveSchemaFiles(final String content, final Path path) {
        Path schemaPath = pathResolver.getSchemaPayloadsDirectory();

        String contentReplaces = content
                .replace(HASH, EMPTY_STRING)
                .replace(COMPONENTS_SCHEMAS, schemaPath.toUri().toString());

        fileInteractor.writeFile(path, contentReplaces);
    }
}
