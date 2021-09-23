package id.global.plugin.model.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;

public class MetadataAnnotator extends Jackson2Annotator {

    private JsonNode channels;

    public MetadataAnnotator(JsonNode node, GenerationConfig generationConfig) {
        super(generationConfig);
        this.channels = node;
    }

    @Override
    public void typeInfo(JDefinedClass clazz, JsonNode schema) {
        super.typeInfo(clazz, schema);
        String exchange = channels.path("bindings").path("amqp").path("exchange").path("name").textValue();
        String exchangeType = channels.path("bindings").path("amqp").path("exchange").path("type").textValue();
        String routingKey = channels.path("bindings").path("amqp").path("queue").path("name").textValue();

        clazz.annotate(id.global.common.annotations.EventMetadata.class)
                .param("exchange", exchange)
                .param("routingKey", routingKey)
                .param("exchangeType", exchangeType);
    }
}
