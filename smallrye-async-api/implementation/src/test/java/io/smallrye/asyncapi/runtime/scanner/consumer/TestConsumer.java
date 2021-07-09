package io.smallrye.asyncapi.runtime.scanner.consumer;

import io.smallrye.asyncapi.runtime.scanner.model.TestModel;
import io.smallrye.asyncapi.spec.annotations.Operation;
import io.smallrye.asyncapi.spec.annotations.Parameter;
import io.smallrye.asyncapi.spec.annotations.channels.ChannelItem;
import io.smallrye.asyncapi.spec.annotations.components.Message;
import io.smallrye.asyncapi.spec.annotations.enums.SchemaType;
import io.smallrye.asyncapi.spec.annotations.media.Schema;

@ChannelItem(name = "test/some/queue/path/{testId}", description = "Just a test consumer", parameters = {
        @Parameter(name = "testId", description = "a parameter", schema = @Schema(type = SchemaType.STRING))
}, subscribe = @Operation(operationId = "consumeSomeQueueEvent", description = "consumer subscribe operation", message = @Message(name = "consumer subscribe message", payload = @Schema(implementation = TestModel.class))))
public class TestConsumer {
    public void handleMessage(TestModel message) {
        // process message

    }
}
