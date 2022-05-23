package id.global.iris.plugin.model.generator.utils;

import java.util.regex.Pattern;

public class ExistingJavaTypeProcessor {
    private static final Pattern EXISTING_JAVA_TYPE_CANDIDATE_PATTERN = Pattern.compile(
            "^.*javaType\" : \"([^\"]*)(\".\s+},.\s*\")existingJavaType\" : \"([^\"]*)(.*)$", Pattern.DOTALL);
    private static final Pattern CLASS_NAME_IN_JAVA_TYPE = Pattern.compile("^.*\\.(?:([a-zA-Z]*)(?!\\.))$");
    private static final String REPLACEMENT_PATTERN_TEMPLATE = "((?:[.$a-zA-Z]*)%s)";

    public String fixExistingType(final String schemaContent) {
        final var existingJavaTypeCandidateMatcher = EXISTING_JAVA_TYPE_CANDIDATE_PATTERN.matcher(schemaContent);

        if (!existingJavaTypeCandidateMatcher.matches()) {
            return schemaContent;
        }

        final var javaType = existingJavaTypeCandidateMatcher.group(1);
        final var classNameMatcher = CLASS_NAME_IN_JAVA_TYPE.matcher(javaType);

        if (!classNameMatcher.matches()) {
            return schemaContent;
        }

        final var existingJavaType = existingJavaTypeCandidateMatcher.group(3);
        final var className = classNameMatcher.group(1);
        Pattern replacePattern = getReplacementPattern(className);
        final var newJavaType = existingJavaType.replaceAll(replacePattern.pattern(), javaType);
        return schemaContent.replace(existingJavaType, newJavaType);
    }

    private Pattern getReplacementPattern(final String className) {
        return Pattern.compile(String.format(REPLACEMENT_PATTERN_TEMPLATE, className), Pattern.DOTALL);
    }

}
