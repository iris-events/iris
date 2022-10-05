package id.global.iris.asyncapi.runtime.generator;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import id.global.iris.asyncapi.runtime.io.schema.SchemaConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDefinitionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CustomDefinitionProvider.class);
    private static final Pattern JAVA_UTIL_MAP_PATTERN = Pattern.compile("^(java\\.util\\.)(.*)(Map)(.*)\\<(.+),(.+)\\>$");
    private static final Set<String> DEFAULT_CONVERT_TO_OBJECT_CANDIDATES = Set.of(
            "java.util.Map", "java.util.HashMap", "java.util.TreeMap", "com.fasterxml.jackson.databind.JsonNode");

    /**
     * Will create a CustomDefinitionProviderV2 for converting schema properties to object. Conversion candidates
     * parameter can contain fully qualified class names (i.e. java.util.HashMap) or just packages (i.e. java.util).
     * Do not use wildcards in package prefixes as this matches by using String.startsWith().
     *
     * @param conversionCandidates candidates for conversion to "object" type schema
     * @return an instance of CustomDefinitionProviderV2 with custom definitions for provided conversionCandidates
     */
    public static CustomDefinitionProviderV2 convertTypesToObject(Set<String> conversionCandidates) {
        final var candidates = new HashSet<>(DEFAULT_CONVERT_TO_OBJECT_CANDIDATES);
        candidates.addAll(conversionCandidates);

        return (javaType, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(javaType);

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (!isInExcludeFromSchemas(fullTypeDescription, candidates)) {
                return null;
            } else {
                return new CustomDefinition(generatorConfig.createObjectNode()
                        .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_NULL)));
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
        final var candidates = new HashSet<>(DEFAULT_CONVERT_TO_OBJECT_CANDIDATES);
        candidates.addAll(conversionCandidates);

        return (scope, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(scope.getDeclaredType());

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (!isInExcludeFromSchemas(fullTypeDescription, candidates)) {
                return null;
            } else {
                final var mapTypeOptional = getMapType(fullTypeDescription);

                return mapTypeOptional.map(mapType -> {
                    final var definitionNode = generatorConfig.createObjectNode();
                    definitionNode.put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                            generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
                    definitionNode.put(SchemaConstant.PROP_EXISTING_JAVA_TYPE, fullTypeDescription);
                    return new CustomPropertyDefinition(definitionNode);
                }).orElse(new CustomPropertyDefinition(
                        generatorConfig.createObjectNode()
                                .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                        generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_NULL))));
            }
        };
    }

    private static Optional<String> getMapType(final String fullTypeDescription) {
        Matcher matcher = JAVA_UTIL_MAP_PATTERN.matcher(fullTypeDescription);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        final var mapType = matcher.group(2);
        return Optional.of(mapType + "Map");
    }

    private static boolean isInExcludeFromSchemas(String fullTypeDescription, Set<String> excludeFromSchemas) {
        return excludeFromSchemas.stream().anyMatch(fullTypeDescription::startsWith);
    }
}
