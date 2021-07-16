package io.smallrye.asyncapi.runtime.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Map;

import org.jboss.jandex.Index;
import org.junit.Test;

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiComponents;
import io.apicurio.datamodels.asyncapi.models.AaiMessage;
import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.models.AaiParameter;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.apicurio.datamodels.core.models.common.Info;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.runtime.scanner.app.EventAppMVP;
import io.smallrye.asyncapi.runtime.scanner.app.InlineDefinedAsyncApi;
import io.smallrye.asyncapi.runtime.scanner.consumer.TestConsumer;
import io.smallrye.asyncapi.runtime.scanner.model.TestModel;
import io.smallrye.asyncapi.runtime.scanner.producer.TestProducer;
import io.smallrye.asyncapi.spec.annotations.enums.SchemaType;

public class AsyncApiAnnotationScannerTest extends IndexScannerTestBase {

    @Test
    public void scanInlineDefinedAsyncApi() {
        Index index = indexOf(
                InlineDefinedAsyncApi.class);

        AsyncApiAnnotationScanner scanner = new AsyncApiAnnotationScanner(emptyConfig(), index);
        Aai20Document document = scanner.scan();

        String id = document.id;
        Info info = document.info;

        assertEquals("2.0.0", id);
        assertEquals("Test application", info.title);
        assertEquals("1.0", info.version);
        assertNull(info.description);
        assertNull(info.termsOfService);
        assertNull(info.contact);
        assertNull(info.license);

        Map<String, AaiChannelItem> channels = document.channels;
        assertEquals(1, channels.size());
        AaiChannelItem channel1 = channels.get("channel1");
        assertNotNull(channel1);
        assertNotNull(channel1.subscribe);
        assertNull(channel1.publish);

        AaiOperation subscribe = channel1.subscribe;
        assertEquals("channel1Subscribe", subscribe.operationId);
        assertEquals("subscribe to channel1", subscribe.description);

        AaiMessage message = subscribe.message;
        assertNotNull(message);
        assertEquals("channel1Message", message.name);
        assertEquals("message for channel1 subscription", message.description);

        Object payload = message.payload;
        // This is currently not supported
        assertNull(payload);

    }

    @Test
    public void scanClassAnnotatedConsumersProducersModels() throws ClassNotFoundException, IOException {
        Index testIndex = indexOf(
                TestConsumer.class,
                TestProducer.class,
                TestModel.class,
                EventAppMVP.class);

        AsyncApiAnnotationScanner scanner = new AsyncApiAnnotationScanner(emptyConfig(), testIndex);

        Aai20Document document = scanner.scan();

        assertNotNull(document);

        Map<String, AaiChannelItem> channels = document.channels;
        AaiComponents components = document.components;

        assertNotNull(channels);
        assertNotNull(components);
        assertEquals(2, channels.size());

        AaiChannelItem consumerChannelItem = channels.get("test/some/queue/path/{testId}");
        AaiChannelItem producerChannelItem = channels.get("test/some/queue/path");
        assertNotNull(consumerChannelItem);
        assertNotNull(producerChannelItem);

        assertEquals("Just a test consumer", consumerChannelItem.description);

        AaiOperation subscribeOperation = consumerChannelItem.subscribe;
        Map<String, AaiParameter> channelParameters = consumerChannelItem.parameters;

        assertNotNull(subscribeOperation);
        assertNotNull(channelParameters);
        assertEquals(1, channelParameters.size());
        assertNull(producerChannelItem.parameters);

        AaiParameter testParameter = channelParameters.get("testId");
        assertEquals("a parameter", testParameter.description);
        assertEquals(SchemaType.STRING.toString(), ((AaiSchema) testParameter.schema).type);

        assertEquals("consumeSomeQueueEvent", subscribeOperation.operationId);
        assertEquals("consumer subscribe operation", subscribeOperation.description);

        AaiMessage subscribeMessage = subscribeOperation.message;
        assertNotNull(subscribeMessage);
        assertNotNull(subscribeMessage.payload);
        Schema payload = (Schema) subscribeMessage.payload;
        assertEquals("#/components/schemas/TestModel", payload.$ref);

        Map<String, AaiSchema> schemas = components.schemas;
        assertEquals(4, schemas.size());

        AaiSchema testModel = schemas.get("TestModel");
        AaiSchema user = schemas.get("User");
        AaiSchema status = schemas.get("Status");
        AaiSchema userMap = schemas.get("Map(String,User)");

        assertNotNull(testModel);
        assertNotNull(user);
        assertNotNull(status);
        assertNotNull(userMap);
    }
}
