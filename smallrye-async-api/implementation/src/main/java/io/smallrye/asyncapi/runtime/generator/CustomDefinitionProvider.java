package io.smallrye.asyncapi.runtime.generator;

import java.util.Set;

import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;

public class CustomDefinitionProvider {

    public static final String JAVA_PACKAGE_PREFIX = "java.";

    public static CustomDefinitionProviderV2 convertUnknownTypeToObject(Set<String> ignorePackagePrefixes) {
        final Set<String> ignoredPackages = addMissingJavaPackage(ignorePackagePrefixes);
        return (javaType, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(javaType);

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInIgnoredPackagePrefixes(fullTypeDescription, ignoredPackages)) {
                return null;
            } else {
                return new CustomDefinition(generatorConfig.createObjectNode()
                        .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)));
            }
        };
    }

    public static CustomPropertyDefinitionProvider<FieldScope> convertUnknownFieldToObject(Set<String> ignorePackagePrefixes) {
        final Set<String> ignoredPackages = addMissingJavaPackage(ignorePackagePrefixes);
        return (scope, context) -> {
            String fullTypeDescription = context.getTypeContext().getFullTypeDescription(scope.getDeclaredType());

            SchemaGeneratorConfig generatorConfig = context.getGeneratorConfig();
            if (isInIgnoredPackagePrefixes(fullTypeDescription, ignoredPackages)) {
                return null;
            } else {
                return new CustomPropertyDefinition(
                        generatorConfig.createObjectNode()
                                .put(generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE),
                                        generatorConfig.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)));
            }
        };
    }

    private static boolean isInIgnoredPackagePrefixes(String fullTypeDescription, Set<String> ignorePackagePrefixes) {
        return ignorePackagePrefixes.stream().filter(fullTypeDescription::startsWith).findFirst()
                .orElse(null) != null;
    }

    private static Set<String> addMissingJavaPackage(Set<String> ignoredPackages) {
        ignoredPackages.add(JAVA_PACKAGE_PREFIX);
        return ignoredPackages;
    }
}
