package org.iris_events.plugin.model.generator.utils;

public class AmqpStringUtils {

    public static final String JAVA_TYPE_TEMPLATE = """
            "type":"object",
            "javaType":"%s.%s.%s.%s"
            """
            .trim();

    public static String getRefRegexToBeReplaced(final String fileLocation) {
        return String.format("\"\\$ref\"\\s?:\\s?\"file:\\/\\/%s\"", fileLocation.replaceAll("\\/", "\\\\/"));
    }

    public static String getReplacementForRef(final String name, final String packageName, final String modelName) {
        return String.format(JAVA_TYPE_TEMPLATE, packageName, modelName, StringConstants.PAYLOAD, name);
    }

    public static String getPackageName(final String modelsName) {
        return modelsName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    public static String getPomArtifactId(final String modelsName) {
        return modelsName.replaceAll("/([a-z0-9]|(?=[A-Z]))([_A-Z])/g", "$1-$2").replaceAll("_", "-").toLowerCase();
    }
}
