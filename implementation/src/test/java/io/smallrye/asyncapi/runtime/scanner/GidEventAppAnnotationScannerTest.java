package io.smallrye.asyncapi.runtime.scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.Index;
import org.junit.Test;

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersApp;
import io.smallrye.asyncapi.runtime.scanner.app.EventHandlersBadExampleApp;
import io.smallrye.asyncapi.runtime.scanner.app.FanoutEventHandlersApp;
import io.smallrye.asyncapi.runtime.scanner.model.GidAai20AmqpChannelBindings;
import io.smallrye.asyncapi.runtime.scanner.model.TestEventV2;

public class GidEventAppAnnotationScannerTest extends IndexScannerTestBase {
    @Test
    public void eventAppAnnotationShouldGenerateBasicDocument() {
        Index index = indexOf(EventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.info, notNullValue());
        assertThat(document.components, notNullValue());
        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());

        assertThat(document.id, is(EventHandlersApp.ID));
        assertThat(document.info.title, is(EventHandlersApp.TITLE));
        assertThat(document.info.version, is(EventHandlersApp.VERSION));
    }

    @Test
    public void messageHandlerAnnotationsShouldGenerateChannelsAndSchemas() {
        Index index = indexOf(EventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());
        assertThat(document.channels.size(), is(6));

        Set<Map.Entry<String, AaiChannelItem>> channelEntries = document.channels.entrySet();
        List<Map.Entry<String, AaiChannelItem>> publishChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().publish != null)
                .collect(Collectors.toList());
        List<Map.Entry<String, AaiChannelItem>> subscribeChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().subscribe != null)
                .collect(Collectors.toList());

        assertThat(publishChannels.size(), is(0));
        assertThat(subscribeChannels.size(), is(6));
        assertThat(document.components.schemas.size(), is(7));

        Map.Entry<String, AaiChannelItem> defaultTestEventV1Entry = subscribeChannels.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getKey().equals("/defaultTestEventV1"))
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
                is("defaultTestEventV1"));
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
        Index index = indexOf(FanoutEventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas, notNullValue());
        assertThat(document.channels, notNullValue());

        assertThat(document.components.schemas.size(), is(6));
        assertThat(document.channels.size(), is(3));
    }

    @Test
    public void excludingPackagesFromSchemasShouldNotIncludeThemInFinalDocument() throws ClassNotFoundException {
        Index index = indexOf(EventHandlersApp.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document.components.schemas.size(), is(7));
        assertThat(document.components.schemas.get("JsonNode"), notNullValue());

        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");
        excludedPrefixes.add("com.fasterxml");

        GidAnnotationScanner scannerWithExclude = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes),
                index);
        Aai20Document documentWithExclude = scannerWithExclude.scan();

        assertThat(documentWithExclude.components.schemas.size(), is(5));

        // Finding JsonNode under components.schemas is NOT expected
        assertThat(documentWithExclude.components.schemas.get("JsonNode"), nullValue());
        assertThat(documentWithExclude.components.schemas.get(TestEventV2.class.getSimpleName()).properties.get("payload").type,
                is("null"));
    }

    @Test
    public void hashMapEvent() {
        Index index = indexOf(EventHandlersBadExampleApp.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(2));
        assertThat(document.components.schemas.get("Map(String,Object)"), notNullValue());
    }

    @Test
    public void hashMapEventExcludeFromSchemas() {
        Index index = indexOf(EventHandlersBadExampleApp.class);
        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");

        GidAnnotationScanner scanner = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes), index);
        Aai20Document document = scanner.scan();

        assertThat(document, notNullValue());
        assertThat(document.components.schemas.size(), is(1));
        assertThat(document.components.schemas.get("Map(String,Object)"), nullValue());
    }
}
