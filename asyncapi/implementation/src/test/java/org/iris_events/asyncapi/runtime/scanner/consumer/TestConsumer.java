package org.iris_events.asyncapi.runtime.scanner.consumer;

import org.iris_events.asyncapi.runtime.scanner.model.TestModel;
import org.iris_events.asyncapi.spec.annotations.Operation;
import org.iris_events.asyncapi.spec.annotations.Parameter;
import org.iris_events.asyncapi.spec.annotations.channels.ChannelItem;
import org.iris_events.asyncapi.spec.annotations.components.Message;
import org.iris_events.asyncapi.spec.annotations.enums.SchemaType;
import org.iris_events.asyncapi.spec.annotations.media.Schema;

@ChannelItem(name = "test/some/queue/path/{testId}", description = "Just a test consumer", parameters = {
        @Parameter(name = "testId", description = "a parameter", schema = @Schema(type = SchemaType.STRING)) }, subscribe = @Operation(operationId = "consumeSomeQueueEvent", description = "consumer subscribe operation", message = @Message(name = "consumer subscribe message", payload = @Schema(implementation = TestModel.class))))
public class TestConsumer {
    public void handleMessage(TestModel message) {
        // process message

    }
}
