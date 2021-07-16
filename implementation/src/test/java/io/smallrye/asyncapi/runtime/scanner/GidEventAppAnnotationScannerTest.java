package io.smallrye.asyncapi.runtime.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import io.smallrye.asyncapi.runtime.scanner.app.FanoutEventHandlersApp;
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
        assertEquals(5, document.components.schemas.size());
        assertEquals(3, document.channels.size());
    }

    @Test
    public void addingPackagePrefixShouldConvertOtherPackageClassesToObjectType() throws ClassNotFoundException {
        Index index = indexOf(EventHandlersApp.class);

        GidAnnotationScanner scanner = new GidAnnotationScanner(testConfig("io.smallrye"), index);
        Aai20Document document = scanner.scan();

        assertEquals(6, document.components.schemas.size());

        // Finding JsonNode under components.schemas is NOT expected
        assertNull(document.components.schemas.get("JsonNode"));
        assertEquals("object",
                document.components.schemas.get(TestEventV2.class.getSimpleName()).properties.get("payload").type);
    }
}
