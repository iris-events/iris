package id.global.plugin.model.generator.utils;

public class StringReplacement {

    public static final String JAVA_TYPE_TEMPLATE = """
            "type": "object",
            "javaType": "%s.%s.%s"
            }
            """
            .trim();

    public static final String REF_FILE_TEMPLATE = """
            "$ref":"file://%s"}"""
            .trim();

    public static String getRefToBeReplaced(final String fileLocation) {
        return String.format(REF_FILE_TEMPLATE, fileLocation);
    }


    public static String getReplacementForRef(final String name, final String packageName, final String modelName) {
        return String.format(JAVA_TYPE_TEMPLATE, packageName, modelName, name);
    }
}
