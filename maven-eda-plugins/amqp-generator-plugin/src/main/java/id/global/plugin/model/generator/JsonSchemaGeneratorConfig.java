package id.global.plugin.model.generator;

import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;

public class JsonSchemaGeneratorConfig {
    public static final GenerationConfig config = new DefaultGenerationConfig() {
        @Override
        public boolean isGenerateBuilders() { // set config option by overriding method
            return true;
        }

        @Override
        public boolean isUseInnerClassBuilders() {
            return true;
        }

        @Override
        public boolean isIncludeDynamicBuilders() {
            return true;
        }

        @Override
        public boolean isSerializable() {
            return true;
        }

        @Override
        public boolean isIncludeAdditionalProperties() {
            return false;
        }
    };
}
