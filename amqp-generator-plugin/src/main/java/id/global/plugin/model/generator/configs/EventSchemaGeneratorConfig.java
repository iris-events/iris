package id.global.plugin.model.generator.configs;

import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;

public class EventSchemaGeneratorConfig {
    public static final GenerationConfig eventConfig = new DefaultGenerationConfig() {
        @Override
        public boolean isGenerateBuilders() { // set config option by overriding method
            return false;
        }

        @Override
        public boolean isUseInnerClassBuilders() {
            return false;
        }

        @Override
        public boolean isIncludeDynamicBuilders() {
            return false;
        }

        @Override
        public boolean isSerializable() {
            return true;
        }

        @Override
        public boolean isIncludeAdditionalProperties() {
            return true;
        }

        @Override
        public boolean isUsePrimitives() {
            return true;
        }

        @Override
        public String getDateTimeType() {
            return "java.time.ZonedDateTime";
        }

        @Override
        public String getDateType() {
            return "java.time.LocalDate";
        }

        @Override
        public boolean isUseBigDecimals() {
            return true;
        }

        @Override
        public boolean isIncludeJsr305Annotations() {
            return true;
        }

        @Override
        public boolean isIncludeJsr303Annotations() {
            return true;
        }

        @Override
        public boolean isIncludeConstructors() {
            return true;
        }

        @Override
        public boolean isIncludeRequiredPropertiesConstructor() {
            return true;
        }
    };

    public static final GenerationConfig classConfig = new DefaultGenerationConfig() {
        @Override
        public boolean isGenerateBuilders() { // set config option by overriding method
            return false;
        }

        @Override
        public boolean isUseInnerClassBuilders() {
            return false;
        }

        @Override
        public boolean isIncludeDynamicBuilders() {
            return false;
        }

        @Override
        public boolean isSerializable() {
            return true;
        }

        @Override
        public boolean isIncludeAdditionalProperties() {
            return false;
        }

        @Override
        public boolean isUsePrimitives() {
            return true;
        }

        @Override
        public String getDateTimeType() {
            return "java.time.ZonedDateTime";
        }

        @Override
        public String getDateType() {
            return "java.time.LocalDate";
        }

        @Override
        public boolean isUseBigDecimals() {
            return true;
        }

        @Override
        public boolean isIncludeJsr305Annotations() {
            return true;
        }

        @Override
        public boolean isIncludeJsr303Annotations() {
            return true;
        }

        @Override
        public boolean isIncludeConstructors() {
            return true;
        }

        @Override
        public boolean isIncludeRequiredPropertiesConstructor() {
            return true;
        }
    };
}
