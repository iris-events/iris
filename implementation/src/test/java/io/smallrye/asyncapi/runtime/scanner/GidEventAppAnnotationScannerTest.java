package io.smallrye.asyncapi.runtime.scanner;

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

import org.hamcrest.Matchers;
import org.jboss.jandex.Index;
import org.junit.Test;

import id.global.asyncapi.spec.enums.Scope;
import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersApp;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersAppWithSentEvent;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersBadExampleApp;
import io.smallrye.asyncapi.runtime.scanner.app.FanoutEventHandlersApp;
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
    public void generatedMessagesShouldHaveTitles() {
        Index index = getEventHandlersAppIndex();

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());
        assertThat(document.channels.size(), is(5));

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
        assertThat(document.channels, notNullValue());
        assertThat(document.channels.size(), is(5));

        Set<Map.Entry<String, AaiChannelItem>> channelEntries = document.channels.entrySet();
        List<Map.Entry<String, AaiChannelItem>> publishChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().publish != null)
                .collect(Collectors.toList());
        List<Map.Entry<String, AaiChannelItem>> subscribeChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().subscribe != null)
                .collect(Collectors.toList());

        assertThat(publishChannels.size(), is(0));
        assertThat(subscribeChannels.size(), is(5));
        assertThat(document.components.schemas.size(), is(9));

        Map.Entry<String, AaiChannelItem> defaultTestEventV1Entry = subscribeChannels.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getKey().equals("/default-test-event-v1"))
                .collect(Collectors.toList()).get(0);

        assertThat(defaultTestEventV1Entry.getValue(), notNullValue());
        assertThat(defaultTestEventV1Entry.getValue().subscribe, notNullValue());
        assertThat(defaultTestEventV1Entry.getValue().bindings, notNullValue());
        assertThat(defaultTestEventV1Entry.getValue().bindings.amqp, notNullValue());

        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getIs(),
                is("routingKey"));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange(),
                notNullValue());
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue(), notNullValue());

        assertThat(
                ((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("durable"),
                is(true));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("vhost"),
                is("/"));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("name"),
                is(""));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange()
                .get("autoDelete"), is(false));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("type"),
                is("direct"));

        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue(), notNullValue());
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue().get("durable"),
                is(true));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue().get("vhost"),
                is("/"));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue().get("name"),
                is("default-test-event-v1"));
        assertThat(
                ((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue().get("autoDelete"),
                is(false));
        assertThat(((GidAai20AmqpChannelBindings) defaultTestEventV1Entry.getValue().bindings.amqp).getQueue().get("exclusive"),
                is(false));

        // Finding JsonNode under components.schemas is expected in this case
        AaiSchema jsonNodeSchema = document.components.schemas.get("JsonNode");
        assertThat(jsonNodeSchema, notNullValue());
    }

    @Test
    public void fanoutMessageHandlerAnnotationsShouldGenerateChannelsAndSchemas() {
        Index index = indexOf(FanoutEventHandlersApp.class,
                FanoutEventHandlersApp.TestEventV1.class,
                FanoutEventHandlersApp.TestEventV2.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());
        assertThat(document.components.schemas.size(), is(6));
        assertThat(document.channels.size(), is(2));
    }

    @Test
    public void excludingPackagesFromSchemasShouldNotIncludeThemInFinalDocument() {
        Index index = getEventHandlersAppIndex();
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas.size(), is(9));
        assertThat(document.components.schemas.get("JsonNode"), notNullValue());

        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");
        excludedPrefixes.add("com.fasterxml");

        GidAnnotationScanner scannerWithExclude = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes),
                index);
        Aai20Document documentWithExclude = scannerWithExclude.scan();

        assertThat(documentWithExclude.components.schemas.size(), is(7));

        // Finding JsonNode under components.schemas is NOT expected
        assertThat(documentWithExclude.components.schemas.get("JsonNode"), nullValue());
        assertThat(
                documentWithExclude.components.schemas.get(EventHandlersApp.TestEventV2.class.getSimpleName()).properties.get(
                        "payload").type,
                is("null"));
    }

    @Test
    public void hashMapEvent() {
        Index index = indexOf(EventHandlersBadExampleApp.class, EventHandlersBadExampleApp.MapEvent.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(2));
        assertThat(document.components.schemas.get("Map(String,Object)"), notNullValue());
    }

    @Test
    public void hashMapEventExcludeFromSchemas() {
        Index index = indexOf(EventHandlersBadExampleApp.class, EventHandlersBadExampleApp.MapEvent.class);
        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");

        GidAnnotationScanner scanner = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes), index);
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(1));
        assertThat(document.components.schemas.get("Map(String,Object)"), nullValue());
    }

    @Test
    public void enumValuesShouldNotIgnoreJsonProperty() {
        Index index = indexOf(
                EventHandlersAppWithSentEvent.class,
                EventHandlersAppWithSentEvent.SentEvent.class
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

    //    @Test
    //    public void generatedModelClassShouldNotGenerateComponentSchema() {
    //        Index index = indexOf(EventHandlersAppWithGeneratedEvents.class);
    //        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
    //        Aai20Document document = scanner.scan();
    //
    //        assertThat(document, is(notNullValue()));
    //        assertThat(document.components.schemas, is(notNullValue()));
    //        assertThat(document.components.schemas.size(), is(0));
    //    }

    @Test
    public void producedEventAnnotatedClassShouldGenerateComponentSchema() {
        Index index = indexOf(
                SentEvent.class
                //                ProducedEvent.class,
                //                MessageHandler.class,
                //                TopicMessageHandler.class,
                //                FanoutMessageHandler.class
        );
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
    public void sameEventProducedAndHandledShouldBeOneSchema() {
        Index index = indexOf(
                EventHandlersAppWithSentEvent.class,
                EventHandlersAppWithSentEvent.SentEvent.class
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
                EventHandlersAppWithSentEvent.SentEvent.class
        );
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, is(notNullValue()));
        assertThat(document.channels.size(), is(2));

        assertThat(document.channels.get("/sent-event-v1").subscribe, is(notNullValue()));
        assertThat(document.channels.get("/sent-event-v1").subscribe.message.getExtraProperty("scope"), is(Scope.INTERNAL));
        assertThat(((Schema) document.channels.get("/sent-event-v1").subscribe.message.payload).$ref,
                is("#/components/schemas/SentEvent"));

        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish, is(notNullValue()));
        assertThat(document.channels.get("sent-event-exchange/sent-event-queue").publish.message.getExtraProperty("scope"),
                is(Scope.EXTERNAL));
        assertThat(((Schema) document.channels.get("sent-event-exchange/sent-event-queue").publish.message.payload).$ref,
                is("#/components/schemas/SentEvent"));
    }

    @Test
    public void eventWithRolesAllowedShouldGenerateMessageProperties() {
        Index index = indexOf(
                SentEvent.class
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
                EventHandlersApp.FanoutTestEventV1.class);
    }
}
