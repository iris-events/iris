package org.iris_events.asyncapi.runtime;

import java.util.UUID;

import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;
import org.iris_events.asyncapi.runtime.scanner.model.GidAsyncApi26Schema;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26BindingImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ChannelsImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26InfoImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26OperationImpl;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26SchemaImpl;

public class ApicurioModelsTest {

    @Test
    public void testApicurioModels() throws JsonProcessingException {
        final var document = generateBaseDocument(UUID.randomUUID().toString(), "Test document", "1.0.0");

        // add components
        final var documentWcomponents = addComponents(document);

        // add channels
        final var documentWchannels = addChannels(documentWcomponents);

        //        Library.validate();
        final var documentJsonNode = Library.writeDocument(documentWchannels);
        final var schemaString = IrisObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(documentJsonNode);
        System.out.println("Generated schema:\n" + schemaString);
    }

    private AsyncApi26Document addComponents(final AsyncApi26Document document) {
        final AsyncApiComponents components = new AsyncApi26ComponentsImpl();

        final GidAsyncApi26Schema eventSchema = new GidAsyncApi26Schema();

        eventSchema.setDescription("Event description");
        eventSchema.setIrisGenerated(true);
        final GidAsyncApi26Schema ageSchema = new GidAsyncApi26Schema();
        ageSchema.setDescription("Age of the user");
        ageSchema.setType("integer");
        eventSchema.addProperty("age", ageSchema);
        components.addSchema("EventName", eventSchema);

        document.setComponents(components);
        return document;
    }

    private AsyncApi26Document addChannels(final AsyncApi26Document document) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        final AsyncApiChannels channels = new AsyncApi26ChannelsImpl();

        final var channelItem = channels.createChannelItem();
        final AsyncApiOperation publishOperation = new AsyncApi26OperationImpl();

        final AsyncApiMessage message = new AsyncApi26MessageImpl();
        final AsyncApiSchema messageHeaders = new AsyncApi26SchemaImpl();
        messageHeaders.setType("object");
        final Schema xScopeSchema = new AsyncApi26SchemaImpl();
        xScopeSchema.setDescription("Message scope. Default is INTERNAL");
        xScopeSchema.addExtraProperty("type", TextNode.valueOf("string"));

        messageHeaders.addProperty("x-scope", xScopeSchema);

        message.setHeaders(messageHeaders);
        message.setName("Test message name");
        message.setTitle("Test message title");
        message.setPayload(mapper.readTree("{\"$ref\" : \"#/components/schemas/EventName\"}"));

        publishOperation.setMessage(message);

        final var publishBindings = publishOperation.createOperationBindings();
        final AsyncApiBinding amqpBindings = new AsyncApi26BindingImpl();
        amqpBindings.setNodeAttribute("deliveryMode", 2);
        publishBindings.setAmqp(amqpBindings);

        publishOperation.setBindings(publishBindings);
        channelItem.setPublish(publishOperation);

        channels.addItem("exchange/routingKey", channelItem);

        document.setChannels(channels);
        return document;
    }

    private AsyncApi26Document generateBaseDocument(final String id, final String title, final String version) {
        final var document = (AsyncApi26Document) Library.createDocument(ModelType.ASYNCAPI26);
        document.setAsyncapi("2.6.0");
        document.setId(id);
        document.setDefaultContentType("application/json");

        final Info info = new AsyncApi26InfoImpl();
        info.setTitle(title);
        info.setVersion(version);
        document.setInfo(info);

        return document;
    }

}
