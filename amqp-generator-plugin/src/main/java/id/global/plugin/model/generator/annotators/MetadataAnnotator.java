package id.global.plugin.model.generator.annotators;

import static id.global.plugin.model.generator.utils.StringConstants.EMPTY_STRING;

import java.util.Optional;

import id.global.common.annotations.amqp.Message;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.sun.codemodel.JDefinedClass;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.GlobalIdGenerated;

public class MetadataAnnotator extends Jackson2Annotator {

    public static final String EXCHANGE = "exchange";
    public static final String EXCHANGE_TYPE = "exchangeType";
    public static final String ROUTING_KEY = "routingKey";
    public static final String MESSAGE = "message";
    public static final String SUBSCRIBE = "subscribe";
    public static final String PUBLISH = "publish";
    public static final String QUEUE = "queue";
    public static final String BINDINGS = "bindings";
    public static final String AMQP = "amqp";
    public static final String NAME = "name";
    public static final String TYPE = "type";

    private final Log log;
    private final JsonNode channel;

    public MetadataAnnotator(JsonNode node, GenerationConfig generationConfig) {
        super(generationConfig);
        this.channel = node;
        this.log = new DefaultLog(new ConsoleLogger());
    }

    // NOTE: 3. 10. 21 https://github.com/asyncapi/bindings/tree/master/amqp
    @Override
    public void typeInfo(JDefinedClass clazz, JsonNode schema) {
        super.typeInfo(clazz, schema);

        JsonNode bindingsAmqp = channel.path(BINDINGS).path(AMQP);

        if (bindingsAmqp.toString().equalsIgnoreCase(EMPTY_STRING)) {
            log.warn("There is no AMQP section in asyncapi document!");
            return;
        }

        JsonNode bindingsExchange = bindingsAmqp.path(EXCHANGE);
        JsonNode bindingsQueue = bindingsAmqp.path(QUEUE);
        JsonNode subscribeMessage = channel.path(SUBSCRIBE).path(MESSAGE);
        JsonNode publishMessage = channel.path(PUBLISH).path(MESSAGE);

        //EXCHANGE
        String exchangeName = Optional.ofNullable(bindingsExchange.path(NAME).textValue()).orElseThrow();
        ExchangeType exchangeType = ExchangeType.fromType(
                Optional.ofNullable(bindingsExchange.path(TYPE).textValue()).orElse(""));
        // TODO this is commented out, because some of it will be added to ProducedEvent and ConsumedEvent annotations
        //        boolean exchangeDurable = Optional.of(bindingsExchange.path("durable").booleanValue()).orElse(Boolean.TRUE);
        //        boolean exchangeAutoDelete = Optional.of(bindingsExchange.path("autoDelete").booleanValue()).orElse(Boolean.FALSE);
        //        String exchangeVhost = Optional.ofNullable(bindingsExchange.path("vhost").textValue()).orElse("/");

        //QUEUE
        String routingKey = Optional.ofNullable(bindingsQueue.path(NAME).textValue()).orElseThrow();
        // TODO this is commented out, because some of it will be added to ProducedEvent and ConsumedEvent annotations
        //        boolean queueDurable = Optional.of(bindingsQueue.path("durable").booleanValue()).orElse(Boolean.TRUE);
        //        boolean queueExclusive = Optional.of(bindingsQueue.path("exclusive").booleanValue()).orElse(Boolean.FALSE);
        //        boolean queueAutoDelete = Optional.of(bindingsQueue.path("autoDelete").booleanValue()).orElse(Boolean.FALSE);
        //        String queueVhost = Optional.ofNullable(bindingsQueue.path("vhost").textValue()).orElse("/");

        clazz.annotate(GlobalIdGenerated.class);

        if (!(publishMessage instanceof MissingNode)) {
            clazz.annotate(Message.class)
                    .param(EXCHANGE, exchangeName)
                    .param(EXCHANGE_TYPE, exchangeType)
                    .param(ROUTING_KEY, routingKey);
        }

        if (!(subscribeMessage instanceof MissingNode)) {
            clazz.annotate(Message.class)
                    .param(EXCHANGE, exchangeName)
                    .param(EXCHANGE_TYPE, exchangeType)
                    .param(ROUTING_KEY, routingKey);
        }
    }
}



