package id.global.iris.plugin.model.generator.utils;

public class StringConstants {
    public static final String POM_XML = "pom.xml";
    public static final String POM_TEMPLATE_XML = "pom-template.xml";
    public static final String SUBSCRIBE = "subscribe";
    public static final String PUBLISH = "publish";
    public static final String REF = "$ref";
    public static final String JAVA_TYPE = "javaType";
    public static final String COMMA = ",";
    public static final String DOT = ".";
    public static final String DOT_REGEX = "\\.";
    public static final String FORWARD_SLASH = "/";
    public static final String EMPTY_STRING = "";
    public static final String HASH = "#";
    public static final String COMPONENTS_SCHEMAS = "/components/schemas/";
    public static final String PAYLOAD = "payload";
    public static final String SCHEMAS = "schemas";
    public static final String MODELS = "models";

    public static final String REF_REGEX = """
            ("\\$ref"\\s*:\\s*"(.*?)"\\s*})(?!\\w)
            """
            .trim();

}
