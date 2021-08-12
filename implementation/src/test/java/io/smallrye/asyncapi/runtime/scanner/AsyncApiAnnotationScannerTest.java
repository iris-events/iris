package io.smallrye.asyncapi.runtime.scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(id, is("2.0.0"));
        assertThat(info.title, is("https://global.id/test-application"));
        assertThat(info.version, is("1.0"));
        assertThat(info.description, nullValue());
        assertThat(info.termsOfService, nullValue());
        assertThat(info.contact, nullValue());
        assertThat(info.license, nullValue());

        Map<String, AaiChannelItem> channels = document.channels;

        assertThat(channels.size(), is(1));
        AaiChannelItem channel1 = channels.get("channel1");
        assertThat(channel1, notNullValue());
        assertThat(channel1.subscribe, notNullValue());
        assertThat(channel1.publish, nullValue());

        AaiOperation subscribe = channel1.subscribe;
        assertThat(subscribe.operationId, is("channel1Subscribe"));
        assertThat(subscribe.description, is("subscribe to channel1"));

        AaiMessage message = subscribe.message;
        assertThat(message, notNullValue());
        assertThat(message.name, is("channel1Message"));
        assertThat(message.description, is("message for channel1 subscription"));

        Object payload = message.payload;
        // This is currently not supported
        assertThat(payload, nullValue());

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

        assertThat(document, notNullValue());

        Map<String, AaiChannelItem> channels = document.channels;
        AaiComponents components = document.components;

        assertThat(channels, notNullValue());
        assertThat(components, notNullValue());
        assertThat(channels.size(), is(2));

        AaiChannelItem consumerChannelItem = channels.get("test/some/queue/path/{testId}");
        AaiChannelItem producerChannelItem = channels.get("test/some/queue/path");
        assertThat(consumerChannelItem, notNullValue());
        assertThat(producerChannelItem, notNullValue());

        assertThat(consumerChannelItem.description, is("Just a test consumer"));

        AaiOperation subscribeOperation = consumerChannelItem.subscribe;
        Map<String, AaiParameter> channelParameters = consumerChannelItem.parameters;

        assertThat(subscribeOperation, notNullValue());
        assertThat(channelParameters, notNullValue());
        assertThat(channelParameters.size(), is(1));
        assertThat(producerChannelItem.parameters, nullValue());

        AaiParameter testParameter = channelParameters.get("testId");
        assertThat(testParameter.description, is("a parameter"));
        assertThat(((AaiSchema) testParameter.schema).type, is(SchemaType.STRING.toString()));

        assertThat(subscribeOperation.operationId, is("consumeSomeQueueEvent"));
        assertThat(subscribeOperation.description, is("consumer subscribe operation"));

        AaiMessage subscribeMessage = subscribeOperation.message;
        assertThat(subscribeMessage, notNullValue());
        assertThat(subscribeMessage.payload, notNullValue());
        Schema payload = (Schema) subscribeMessage.payload;
        assertThat(payload.$ref, is("#/components/schemas/TestModel"));

        Map<String, AaiSchema> schemas = components.schemas;
        assertThat(schemas.size(), is(4));

        AaiSchema testModel = schemas.get("TestModel");
        AaiSchema user = schemas.get("User");
        AaiSchema status = schemas.get("Status");
        AaiSchema userMap = schemas.get("Map(String,User)");

        assertThat(testModel, notNullValue());
        assertThat(user, notNullValue());
        assertThat(status, notNullValue());
        assertThat(userMap, notNullValue());
    }
}
