package id.global.asyncapi.runtime.generator;

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

    /**
     * Will create a CustomDefinitionProviderV2 for converting schema properties to object. Conversion candidates
     * parameter can contain fully qualified class names (i.e. java.util.HashMap) or just packages (i.e. java.util).
     * Do not use wildcards in package prefixes as this matches by using String.startsWith().
     *
     * @param conversionCandidates candidates for conversion to "object" type schema
     * @return an instance of CustomDefinitionProviderV2 with custom definitions for provided conversionCandidates
     */
    public static CustomDefinitionProviderV2 convertTypesToObject(Set<String> conversionCandidates) {
        return (javaType, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(javaType);

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInExcludeFromSchemas(fullTypeDescription, conversionCandidates)) {
                return new CustomDefinition(generatorConfig.createObjectNode()
                        .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)));
            } else {
                LOG.info("Ignoring type from schema: " + fullTypeDescription);
                return null;
            }
        };
    }

    /**
     * Will create a CustomDefinitionProviderV2 for converting schema properties to object. Conversion candidates
     * parameter can contain fully qualified class names (i.e. java.util.HashMap) or just packages (i.e. java.util).
     * Do not use wildcards in package prefixes as this matches by using String.startsWith().
     *
     * @param conversionCandidates candidates for conversion to "object" type schema
     * @return instance of a CustomPropertyDefinitionProvider with custom definitions for provided conversionCandidates
     */
    public static CustomPropertyDefinitionProvider<FieldScope> convertFieldsToObject(Set<String> conversionCandidates) {
        return (scope, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(scope.getDeclaredType());

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInExcludeFromSchemas(fullTypeDescription, conversionCandidates)) {
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
        return excludeFromSchemas.stream().anyMatch(fullTypeDescription::startsWith);
    }
}
