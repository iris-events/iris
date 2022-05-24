package id.global.iris.plugin.model.generator.annotators;

import static id.global.iris.plugin.model.generator.utils.StringConstants.EMPTY_STRING;

import java.util.Optional;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JDefinedClass;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.GlobalIdGenerated;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.Scope;
import id.global.iris.asyncapi.api.Headers;

public class MetadataAnnotator extends Jackson2Annotator {

    private static final String EXCHANGE = "exchange";
    private static final String EXCHANGE_TYPE = "exchangeType";
    private static final String ROUTING_KEY = "routingKey";
    private static final String MESSAGE = "message";
    private static final String SUBSCRIBE = "subscribe";
    private static final String PUBLISH = "publish";
    private static final String QUEUE = "queue";
    private static final String BINDINGS = "bindings";
    private static final String AMQP = "amqp";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String HEADERS = "headers";
    private static final String PROPERTIES = "properties";
    private static final String SCOPE = "scope";
    private static final String DEAD_LETTER = "deadLetter";
    private static final String TTL = "ttl";
    public static final String VALUE = "value";

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

        // MESSAGE HEADERS
        JsonNode headers = null;
        if (!publishMessage.isMissingNode()) {
            headers = publishMessage.path(HEADERS);
        } else if (!subscribeMessage.isMissingNode()) {
            headers = subscribeMessage.path(HEADERS);
        }
        Optional<Scope> scope = getScope(headers);
        Optional<String> deadLetter = getDeadLetter(headers);
        Optional<Integer> ttl = getTtl(headers);

        //EXCHANGE
        String exchangeName = Optional.ofNullable(bindingsExchange.path(NAME).textValue()).orElseThrow();
        Optional<ExchangeType> exchangeType = getExchangeType(bindingsExchange.path(TYPE));
        Optional<String> routingKey = Optional.ofNullable(bindingsQueue.path(NAME).textValue());

        clazz.annotate(GlobalIdGenerated.class);

        JAnnotationUse annotatedClazz = clazz.annotate(Message.class)
                .param(NAME, exchangeName);

        // optionals
        exchangeType.ifPresent(type -> annotatedClazz.param(EXCHANGE_TYPE, type));
        routingKey.ifPresent(s -> annotatedClazz.param(ROUTING_KEY, s));
        scope.ifPresent(s -> annotatedClazz.param(SCOPE, s));
        deadLetter.ifPresent(s -> annotatedClazz.param(DEAD_LETTER, s));
        ttl.ifPresent(s -> annotatedClazz.param(TTL, s));
    }

    private Optional<ExchangeType> getExchangeType(JsonNode typeNode) {
        if (typeNode.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(ExchangeType.fromType(typeNode.textValue()));
    }

    private Optional<Integer> getTtl(JsonNode headers) {
        JsonNode extensions = getProperties(headers);
        JsonNode ttlNode = extensions.path(Headers.HEADER_TTL).path(VALUE);

        if (ttlNode.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(ttlNode.asInt());
    }

    private Optional<String> getDeadLetter(JsonNode headers) {
        JsonNode extensions = getProperties(headers);
        if (extensions.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(extensions.path(Headers.HEADER_DEAD_LETTER).path(VALUE).textValue());
    }

    private Optional<Scope> getScope(JsonNode headers) {
        JsonNode properties = getProperties(headers);
        if (properties.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(properties.path(Headers.HEADER_SCOPE).path(VALUE).textValue()).map(Scope::valueOf);
    }

    private JsonNode getProperties(JsonNode headers) {
        return headers.path(PROPERTIES);
    }
}
