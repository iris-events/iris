package io.smallrye.asyncapi.runtime.scanner;

import static io.smallrye.asyncapi.runtime.util.GidAnnotationParser.camelToKebabCase;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import id.global.common.annotations.amqp.Message;
import org.hamcrest.Matchers;
import org.jboss.jandex.Index;
import org.junit.Test;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;
import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.runtime.scanner.app.DummyEventHandlersApp;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersApp;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersAppWithSentEvent;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersBadExampleApp;
import io.smallrye.asyncapi.runtime.scanner.model.AaiSchemaAdditionalProperties;
import io.smallrye.asyncapi.runtime.scanner.model.GidAai20AmqpChannelBindings;
import io.smallrye.asyncapi.runtime.scanner.model.SentEvent;

public class GidEventAppAnnotationScannerTest extends IndexScannerTestBase {

    public static final String ROLES_ALLOWED = "rolesAllowed";

    @Test
    public void eventAppAnnotationShouldGenerateBasicDocument() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.info, notNullValue());
        assertThat(document.components, notNullValue());
        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());

        assertThat(document.id, is("urn:id:global:eventhandlersapptest"));
        assertThat(document.info.title, is("Event handlers"));
        assertThat(document.info.version, is(EventHandlersApp.VERSION));
    }

    @Test
    public void shouldGenerateChannelsCorrectly() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());
        assertThat(document.channels.size(), is(7));

        document.channels.forEach((key, value) -> {
            AaiOperation subscribe = value.subscribe;
            AaiOperation publish = value.publish;

            if (subscribe != null) {
                assertThat(subscribe.message.title, notNullValue());
                assertThat(subscribe.message._name, is(subscribe.message.title));
            }

            if (publish != null) {
                assertThat(publish.message.title, notNullValue());
                assertThat(publish.message._name, is(publish.message.title));
            }
        });

    }

    @Test
    public void generatedIdShouldConformToUri() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        String id = document.id;

        URI url = null;

        try {
            url = new URI(id);
        } catch (URISyntaxException e) {
            fail("Id is not a valid URI");
        }

        assertThat(url, notNullValue());
        assertThat(url.toString().length(), is(Matchers.greaterThan(0)));
    }

    @Test
    public void messageHandlerAnnotationsShouldGenerateChannelsAndSchemas() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());

        Set<Map.Entry<String, AaiChannelItem>> channelEntries = document.channels.entrySet();
        List<Map.Entry<String, AaiChannelItem>> publishChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().publish != null)
                .collect(Collectors.toList());
        List<Map.Entry<String, AaiChannelItem>> subscribeChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().subscribe != null)
                .collect(Collectors.toList());

        assertThat(publishChannels.size(), is(0));
        assertThat(subscribeChannels.size(), is(7));
        assertThat(document.components.schemas.size(), is(11));

        String defaultExchangeName = getDefaultExchangeName(EventHandlersApp.ID, ExchangeType.DIRECT);
        String defaultTestEventV1ChannelName = String.format("%s/default-test-event-v1", defaultExchangeName);
        Map.Entry<String, AaiChannelItem> testEventV1Entry = subscribeChannels.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getKey().equals(defaultTestEventV1ChannelName))
                .collect(Collectors.toList()).get(0);

        AaiChannelItem testEventV1Value = testEventV1Entry.getValue();
        assertThat(testEventV1Value, notNullValue());
        assertThat(testEventV1Value.subscribe, notNullValue());
        assertThat(testEventV1Value.bindings, notNullValue());
        assertThat(testEventV1Value.bindings.amqp, notNullValue());

        GidAai20AmqpChannelBindings testEventGidChannelBindings = (GidAai20AmqpChannelBindings) testEventV1Value.bindings.amqp;
        assertThat(testEventGidChannelBindings.getIs(), is("routingKey"));
        assertThat(testEventGidChannelBindings.getExchange(), notNullValue());
        assertThat(testEventGidChannelBindings.getQueue(), notNullValue());

        assertThat(testEventGidChannelBindings.getExchange().get("durable"), is(true));
        assertThat(testEventGidChannelBindings.getExchange().get("vhost"), is("/"));
        assertThat(testEventGidChannelBindings.getExchange().get("name"), is(defaultExchangeName));
        assertThat(testEventGidChannelBindings.getExchange().get("autoDelete"), is(false));
        assertThat(testEventGidChannelBindings.getExchange().get("type"), is("direct"));

        assertThat(testEventGidChannelBindings.getQueue(), notNullValue());
        assertThat(testEventGidChannelBindings.getQueue().get("durable"), is(true));
        assertThat(testEventGidChannelBindings.getQueue().get("vhost"), is("/"));
        assertThat(testEventGidChannelBindings.getQueue().get("name"), is("default-test-event-v1"));
        assertThat(testEventGidChannelBindings.getQueue().get("autoDelete"), is(false));
        assertThat(testEventGidChannelBindings.getQueue().get("exclusive"), is(false));

        // Event with minimum required annotation values
        Map.Entry<String, AaiChannelItem> testEventDefaultsEntry = subscribeChannels.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getKey()
                        .equals(String.format("%s/event-defaults", defaultExchangeName)))
                .collect(Collectors.toList()).get(0);

        AaiChannelItem testEventDefaultsValue = testEventDefaultsEntry.getValue();

        GidAai20AmqpChannelBindings testEventDefaultGidChannelBindings = (GidAai20AmqpChannelBindings) testEventDefaultsValue.bindings.amqp;
        Map<String, Object> queueValues = testEventDefaultGidChannelBindings.getQueue();
        Map<String, Object> exchangeValues = testEventDefaultGidChannelBindings.getExchange();

        assertThat(queueValues.get("name"), is("event-defaults"));
        assertThat(queueValues.get("durable"), is(true));
        assertThat(queueValues.get("autoDelete"), is(false));
        assertThat(queueValues.get("vhost"), is("/"));

        assertThat(exchangeValues.get("type"), is("direct"));
        assertThat(exchangeValues.get("vhost"), is("/"));
        assertThat(exchangeValues.get("name"), is(defaultExchangeName));
        assertThat(exchangeValues.get("durable"), is(true));
        assertThat(exchangeValues.get("autoDelete"), is(false));

        // Finding JsonNode under components.schemas is expected in this case
        AaiSchema jsonNodeSchema = document.components.schemas.get("JsonNode");
        assertThat(jsonNodeSchema, notNullValue());
    }

    @Test
    public void excludingPackagesFromSchemasShouldNotIncludeThemInFinalDocument() {
        Index index = getEventHandlersAppIndex();
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas.size(), is(11));
        assertThat(document.components.schemas.get("JsonNode"), notNullValue());

        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");
        excludedPrefixes.add("com.fasterxml");

        GidAnnotationScanner scannerWithExclude = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes),
                index);
        Aai20Document documentWithExclude = scannerWithExclude.scan();

        assertThat(documentWithExclude.components.schemas.size(), is(9));

        // Finding JsonNode under components.schemas is NOT expected
        assertThat(documentWithExclude.components.schemas.get("JsonNode"), nullValue());
        assertThat(
                documentWithExclude.components.schemas.get(EventHandlersApp.TestEventV2.class.getSimpleName()).properties.get(
                        "payload").type,
                is("null"));
    }

    @Test
    public void hashMapEvent() {
        Index index = indexOf(EventHandlersBadExampleApp.class, EventHandlersBadExampleApp.MapEvent.class, Message.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(2));
        assertThat(document.components.schemas.get("Map(String,Object)"), notNullValue());
    }

    @Test
    public void hashMapEventExcludeFromSchemas() {
        Index index = indexOf(EventHandlersBadExampleApp.class, EventHandlersBadExampleApp.MapEvent.class, Message.class);
        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");

        GidAnnotationScanner scanner = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes), index
        );
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(1));
        assertThat(document.components.schemas.get("Map(String,Object)"), nullValue());
    }

    @Test
    public void enumValuesShouldNotIgnoreJsonProperty() {
        Index index = indexOf(
                EventHandlersAppWithSentEvent.class,
                EventHandlersAppWithSentEvent.SentEvent.class,
                Message.class
        );
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        AaiSchema status = document.components.schemas.get("Status");

        assertThat(status.type, is("string"));
        assertThat(status.enum_.size(), is(3));
        assertThat(status.enum_.contains("dormant"), is(true));
        assertThat(status.enum_.contains("live"), is(true));
        assertThat(status.enum_.contains("dead"), is(true));
        assertThat(status.enum_.contains("DORMANT"), is(false));
        assertThat(status.enum_.contains("LIVE"), is(false));
        assertThat(status.enum_.contains("DEAD"), is(false));
    }

    @Test
    public void producedEventAnnotatedClassShouldGenerateComponentSchema() {
        Index index = indexOf(DummyEventHandlersApp.class, SentEvent.class, Message.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, is(notNullValue()));
        assertThat(document.components.schemas, is(notNullValue()));
        assertThat(document.components.schemas.size(), is(3));
        assertThat(document.components.schemas.get("SentEvent"), is(notNullValue()));
        assertThat(document.components.schemas.get("Status"), is(notNullValue()));
        assertThat(document.components.schemas.get("User"), is(notNullValue()));

        assertThat(document.components.schemas.get("SentEvent").type, is("object"));
    }

    @Test
    public void generatedEventShouldBeFlagged() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, is(notNullValue()));
        assertThat(document.components.schemas, is(notNullValue()));
        String generatedTestEventName = EventHandlersApp.GeneratedTestEvent.class.getSimpleName();
        assertThat(document.components.schemas.get(generatedTestEventName), is(notNullValue()));
        Object additionalPropertiesObj = document.components.schemas.get(generatedTestEventName).additionalProperties;
        assertThat(additionalPropertiesObj, is(notNullValue()));
        assertThat(((AaiSchemaAdditionalProperties) additionalPropertiesObj).isGeneratedClass(), is(true));
    }

    @Test
    public void sameEventProducedAndHandledShouldBeOneSchema() {
        Index index = indexOf(
                EventHandlersAppWithSentEvent.class,
                EventHandlersAppWithSentEvent.SentEvent.class,
                Message.class
        );
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, is(notNullValue()));
        assertThat(document.components.schemas, is(notNullValue()));
        assertThat(document.components.schemas.size(), is(3));
    }

    @Test
    public void producedAndHandledShouldCreateCorrectChannels() {
        Index index = indexOf(
                EventHandlersAppWithSentEvent.class,
                EventHandlersAppWithSentEvent.SentEvent.class,
                Message.class
        );
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, is(notNullValue()));
        assertThat(document.channels.size(), is(2));

        String sentEventV1ChannelName = String.format("%s/sent-event-v1",
                getDefaultExchangeName(EventHandlersAppWithSentEvent.ID, ExchangeType.DIRECT));
        assertThat(document.channels.get(sentEventV1ChannelName).subscribe, is(notNullValue()));
        assertThat(document.channels.get(sentEventV1ChannelName).subscribe.message.getExtraProperty("scope"),
                is(Scope.INTERNAL));
        assertThat(((Schema) document.channels.get(sentEventV1ChannelName).subscribe.message.payload).$ref,
                is("#/components/schemas/SentEvent"));

        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish, is(notNullValue()));
        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish.message.getExtraProperty("scope"),
                is(Scope.SESSION));
        assertThat(((Schema) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.payload).$ref,
                is("#/components/schemas/SentEvent"));
    }

    @Test
    public void eventWithRolesAllowedShouldGenerateMessageProperties() {
        Index index = indexOf(
                DummyEventHandlersApp.class,
                SentEvent.class,
                Message.class
        );
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();
        assertThat(document, is(notNullValue()));
        assertThat(document.channels.size(), is(1));

        assertThat(
                document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers.getExtension(
                        ROLES_ALLOWED),
                is(notNullValue()));
        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).name, is(ROLES_ALLOWED));
        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).value, is(notNullValue()));
        assertThat(((String[]) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).value).length, is(3));
        assertThat(((String[]) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).value)[0], is("ADMIN"));
        assertThat(((String[]) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).value)[1], is("USER"));
        assertThat(((String[]) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.headers
                .getExtension(ROLES_ALLOWED).value)[2], is("DUMMY"));

    }

    private Index getEventHandlersAppIndex() {
        return indexOf(EventHandlersApp.class,
                EventHandlersApp.TestEventV1.class,
                EventHandlersApp.TestEventV2.class,
                EventHandlersApp.FrontendTestEventV1.class,
                EventHandlersApp.TopicTestEventV1.class,
                EventHandlersApp.FanoutTestEventV1.class,
                EventHandlersApp.GeneratedTestEvent.class,
                EventHandlersApp.EventDefaults.class,
                Message.class);
    }

    private String getDefaultExchangeName(String projectName, ExchangeType exchangeType) {
        return camelToKebabCase(projectName) + "-" + exchangeType.getType();
    }
}
