package io.smallrye.asyncapi.runtime.scanner.app;

import io.smallrye.asyncapi.spec.annotations.AsyncAPIDefinition;
import io.smallrye.asyncapi.spec.annotations.Operation;
import io.smallrye.asyncapi.spec.annotations.channels.ChannelItem;
import io.smallrye.asyncapi.spec.annotations.components.Message;
import io.smallrye.asyncapi.spec.annotations.enums.SchemaType;
import io.smallrye.asyncapi.spec.annotations.info.Info;
import io.smallrye.asyncapi.spec.annotations.media.Schema;
import io.smallrye.asyncapi.spec.annotations.media.SchemaProperty;

@AsyncAPIDefinition(id = "2.0.0", info = @Info(title = "Test application", version = "1.0"), channels = {
        @ChannelItem(name = "channel1", subscribe = @Operation(operationId = "channel1Subscribe", description = "subscribe to channel1", message = @Message(name = "channel1Message", description = "message for channel1 subscription", payload = @Schema( // payloads currently not supported inline, only via implementation class
                properties = {
                        @SchemaProperty(name = "name", type = SchemaType.STRING)
                }))))
})
public class InlineDefinedAsyncApi {
}
