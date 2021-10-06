package id.global.plugin.model.generator.annotators;

import java.util.Optional;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;

import id.global.common.annotations.EventMetadata;
import id.global.common.annotations.ExchangeBindings;
import id.global.common.annotations.QueueBindings;

public class MetadataAnnotator extends Jackson2Annotator {

    private final JsonNode channel;

    public MetadataAnnotator(JsonNode node, GenerationConfig generationConfig) {
        super(generationConfig);
        this.channel = node;
    }

    // NOTE: 3. 10. 21 https://github.com/asyncapi/bindings/tree/master/amqp
    @Override
    public void typeInfo(JDefinedClass clazz, JsonNode schema) {
        super.typeInfo(clazz, schema);

        JsonNode bindingsAmqp = channel.path("bindings").path("amqp");
        JsonNode bindingsExchange = bindingsAmqp.path("exchange");
        JsonNode bindingsQueue = bindingsAmqp.path("queue");
        JsonNode subscribeMessage = channel.path("subscribe").path("message");
        JsonNode publishMessage = channel.path("publish").path("message");

        //Not used at this moment!!
        String subscribeMessageType = subscribeMessage
                .path("_extraProperties").path("type").textValue();
        String subscribeRolesAllowed = subscribeMessage
                .path("headers").path("_extensions").path("rolesAllowed").path("name").textValue();

        String publishMessageType = publishMessage
                .path("_extraProperties").path("type").textValue();
        String publishRolesAllowed = publishMessage
                .path("headers").path("_extensions").path("rolesAllowed").path("name").textValue();

        String is = Optional.ofNullable(bindingsAmqp.path("is").textValue()).orElse("routingKey");
        String bindingVersion = Optional.ofNullable(bindingsAmqp.path("bindingVersion").textValue()).orElse("0.0.1");

        //EXCHANGE
        String exchangeName = Optional.ofNullable(bindingsExchange.path("name").textValue()).orElseThrow();
        String exchangeType = Optional.ofNullable(bindingsExchange.path("type").textValue()).orElse("");
        boolean exchangeDurable = Optional.of(bindingsExchange.path("durable").booleanValue()).orElse(Boolean.TRUE);
        boolean exchangeAutoDelete = Optional.of(bindingsExchange.path("autoDelete").booleanValue()).orElse(Boolean.FALSE);
        String exchangeVhost = Optional.ofNullable(bindingsExchange.path("vhost").textValue()).orElse("/");

        //QUEUE
        String queueName = Optional.ofNullable(bindingsQueue.path("name").textValue()).orElseThrow();
        boolean queueDurable = Optional.of(bindingsQueue.path("durable").booleanValue()).orElse(Boolean.TRUE);
        boolean queueExclusive = Optional.of(bindingsQueue.path("exclusive").booleanValue()).orElse(Boolean.FALSE);
        boolean queueAutoDelete = Optional.of(bindingsQueue.path("autoDelete").booleanValue()).orElse(Boolean.FALSE);
        String queueVhost = Optional.ofNullable(bindingsQueue.path("vhost").textValue()).orElse("/");

        clazz.annotate(EventMetadata.class)
                // Not used at this moment!!
                //.param("is", is)
                //.param("bindingVersion", bindingVersion)
                .param("exchange", exchangeName)
                .param("exchangeType", exchangeType)
                .param("routingKey", queueName);

        clazz.annotate(QueueBindings.class)
                .param("queueName", queueName)
                .param("queueDurable", queueDurable)
                .param("queueExclusive", queueExclusive)
                .param("queueAutoDelete", queueAutoDelete)
                .param("queueVhost", queueVhost);

        clazz.annotate(ExchangeBindings.class)
                .param("exchangeName", exchangeName)
                .param("exchangeType", exchangeType)
                .param("exchangeDurable", exchangeDurable)
                .param("exchangeAutoDelete", exchangeAutoDelete)
                .param("exchangeVhost", exchangeVhost);

    }
}



