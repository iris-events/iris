package io.smallrye.asyncapi.runtime.generator;

import java.util.Set;

import org.jboss.logging.Logger;

import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;

public class CustomDefinitionProvider {
    private static final Logger LOG = Logger.getLogger(CustomDefinitionProvider.class);

    public static CustomDefinitionProviderV2 convertUnknownTypeToObject(Set<String> excludeFromSchemas) {
        return (javaType, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(javaType);

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInExcludeFromSchemas(fullTypeDescription, excludeFromSchemas)) {
                return new CustomDefinition(generatorConfig.createObjectNode()
                        .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)));
            } else {
                LOG.info("Ignoring type from schema: " + fullTypeDescription);
                return null;
            }
        };
    }

    public static CustomPropertyDefinitionProvider<FieldScope> convertUnknownFieldToObject(Set<String> excludeFromSchemas) {
        return (scope, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(scope.getDeclaredType());

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInExcludeFromSchemas(fullTypeDescription, excludeFromSchemas)) {
                return new CustomPropertyDefinition(
                        generatorConfig.createObjectNode()
                                .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                        generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)));
            } else {
                LOG.info("Ignoring field from schema: " + fullTypeDescription);
                return null;
            }
        };
    }

    private static boolean isInExcludeFromSchemas(String fullTypeDescription, Set<String> excludeFromSchemas) {
        return excludeFromSchemas.stream().filter(fullTypeDescription::startsWith).findFirst()
                .orElse(null) != null;
    }
}
