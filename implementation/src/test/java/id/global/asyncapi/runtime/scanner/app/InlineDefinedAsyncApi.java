package id.global.asyncapi.runtime.scanner.app;

import id.global.asyncapi.spec.annotations.channels.ChannelItem;
import id.global.asyncapi.spec.annotations.AsyncAPIDefinition;
import id.global.asyncapi.spec.annotations.Operation;
import id.global.asyncapi.spec.annotations.components.Message;
import id.global.asyncapi.spec.annotations.enums.SchemaType;
import id.global.asyncapi.spec.annotations.info.Info;
import id.global.asyncapi.spec.annotations.media.Schema;
import id.global.asyncapi.spec.annotations.media.SchemaProperty;

@AsyncAPIDefinition(id = "2.0.0", info = @Info(title = "Test application", version = "1.0"), channels = {
        @ChannelItem(name = "channel1", subscribe = @Operation(operationId = "channel1Subscribe", description = "subscribe to channel1", message = @Message(name = "channel1Message", description = "message for channel1 subscription", payload = @Schema( // payloads currently not supported inline, only via implementation class
                properties = {
                        @SchemaProperty(name = "name", type = SchemaType.STRING)
                }))))
})
public class InlineDefinedAsyncApi {
}
