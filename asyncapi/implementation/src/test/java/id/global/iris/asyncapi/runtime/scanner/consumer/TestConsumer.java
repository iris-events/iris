package id.global.iris.asyncapi.runtime.scanner.consumer;

import id.global.iris.asyncapi.runtime.scanner.model.TestModel;
import id.global.iris.asyncapi.spec.annotations.Operation;
import id.global.iris.asyncapi.spec.annotations.Parameter;
import id.global.iris.asyncapi.spec.annotations.channels.ChannelItem;
import id.global.iris.asyncapi.spec.annotations.components.Message;
import id.global.iris.asyncapi.spec.annotations.enums.SchemaType;
import id.global.iris.asyncapi.spec.annotations.media.Schema;

@ChannelItem(name = "test/some/queue/path/{testId}", description = "Just a test consumer", parameters = {
        @Parameter(name = "testId", description = "a parameter", schema = @Schema(type = SchemaType.STRING))
}, subscribe = @Operation(operationId = "consumeSomeQueueEvent", description = "consumer subscribe operation", message = @Message(name = "consumer subscribe message", payload = @Schema(implementation = TestModel.class))))
public class TestConsumer {
    public void handleMessage(TestModel message) {
        // process message

    }
}
