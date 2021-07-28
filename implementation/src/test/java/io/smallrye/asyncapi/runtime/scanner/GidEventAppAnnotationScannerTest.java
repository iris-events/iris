package io.smallrye.asyncapi.runtime.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

        assertNotNull(document.info);
        assertNotNull(document.components);
        assertNotNull(document.components.schemas);
        assertNotNull(document.channels);

        assertEquals(document.id, EventHandlersApp.ID);
        assertEquals(document.info.title, EventHandlersApp.TITLE);
        assertEquals(document.info.version, EventHandlersApp.VERSION);
    }

    @Test
    public void messageHandlerAnnotationsShouldGenerateChannelsAndSchemas() {
        Index index = indexOf(EventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertNotNull(document.components.schemas);
        assertNotNull(document.channels);

        assertEquals(6, document.channels.size());

        Set<Map.Entry<String, AaiChannelItem>> channelEntries = document.channels.entrySet();
        List<Map.Entry<String, AaiChannelItem>> publishChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().publish != null)
                .collect(Collectors.toList());
        List<Map.Entry<String, AaiChannelItem>> subscribeChannels = channelEntries.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getValue().subscribe != null)
                .collect(Collectors.toList());

        assertEquals(0, publishChannels.size()); // for now, as we do not annotate producers, we won't create publish channels
        assertEquals(6, subscribeChannels.size());
        assertEquals(7, document.components.schemas.size());

        Map.Entry<String, AaiChannelItem> defaultTestEventV1Entry = subscribeChannels.stream()
                .filter(stringAaiChannelItemEntry -> stringAaiChannelItemEntry.getKey().equals("/defaultTestEventV1"))
                .collect(Collectors.toList()).get(0);

        assertNotNull(defaultTestEventV1Entry.getValue());
        assertNotNull(defaultTestEventV1Entry.getValue().subscribe);
        assertNotNull(defaultTestEventV1Entry.getValue().bindings);
        assertNotNull(defaultTestEventV1Entry.getValue().bindings.amqp);
        assertEquals(((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getIs(), "routingKey");
        assertNotNull(((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange());
        assertNotNull(((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getQueue());
        assertEquals(true, ((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("durable"));
        assertEquals("/", ((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("vhost"));
        assertEquals("", ((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("name"));
        assertEquals(false, ((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("autoDelete"));
        assertEquals("direct", ((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getExchange().get("type"));

        assertNotNull(((GidAai20AmqpChannelBindings)defaultTestEventV1Entry.getValue().bindings.amqp).getQueue());



        // Finding JsonNode under components.schemas is expected in this case
        AaiSchema jsonNodeSchema = document.components.schemas.get("JsonNode");
        assertNotNull(jsonNodeSchema);
    }

    @Test
    public void fanoutMessageHandlerAnnotationsShouldGenerateChannelsAndSchemas() {
        Index index = indexOf(FanoutEventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertNotNull(document.components.schemas);
        assertNotNull(document.channels);

        // TODO needs serious rework of channel (and bindings for them) creation for
        // fanout and topic exchanges
        assertEquals(6, document.components.schemas.size());
        assertEquals(3, document.channels.size());
    }

    @Test
    public void excludingPackagesFromSchemasShouldNotIncludeThemInFinalDocument() throws ClassNotFoundException {
        Index index = indexOf(EventHandlersApp.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertEquals(7, document.components.schemas.size());
        assertNotNull(document.components.schemas.get("JsonNode"));

        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");
        excludedPrefixes.add("com.fasterxml");

        GidAnnotationScanner scannerWithExclude = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes), index);
        Aai20Document documentWithExclude = scannerWithExclude.scan();

        assertEquals(5, documentWithExclude.components.schemas.size());

        // Finding JsonNode under components.schemas is NOT expected
        assertNull(documentWithExclude.components.schemas.get("JsonNode"));
        assertEquals("object",
                documentWithExclude.components.schemas.get(TestEventV2.class.getSimpleName()).properties.get("payload").type);
    }

    @Test
    public void hashMapEvent() {
        Index index = indexOf(EventHandlersBadExampleApp.class);
        GidAnnotationScanner scanner = new GidAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        assertNotNull(document);
        assertEquals(2, document.components.schemas.size());
        assertNotNull(document.components.schemas.get("Map(String,Object)"));
        // Assert map is in the components
    }

    @Test
    public void hashMapEventExcludeFromSchemas() {
        Index index = indexOf(EventHandlersBadExampleApp.class);
        Set<String> excludedPrefixes = new HashSet<>();
        excludedPrefixes.add("java.util");

        GidAnnotationScanner scanner = new GidAnnotationScanner(excludeFromSchemasTestConfig(excludedPrefixes), index);
        Aai20Document document = scanner.scan();

        assertNotNull(document);
        assertEquals(1, document.components.schemas.size());
        assertNull(document.components.schemas.get("Map(String,Object)"));
        // Assert map is not in the components
    }
}
