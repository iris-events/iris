package io.smallrye.asyncapi.runtime.scanner.producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.asyncapi.runtime.scanner.model.Status;
import io.smallrye.asyncapi.runtime.scanner.model.TestModel;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.Operation;
import io.smallrye.asyncapi.spec.annotations.channels.ChannelItem;
import io.smallrye.asyncapi.spec.annotations.components.Message;
import io.smallrye.asyncapi.spec.annotations.media.Schema;

@ChannelItem(name = "test/some/queue/path", description = "Just a test producer", publish = @Operation(operationId = "produceSomeQueueEvent", message = @Message(name = "someQueueEvent", title = "just a test queue event", contentType = "application/json", payload = @Schema(implementation = TestModel.class))))
public class TestProducer {
    public void produce() {
        User testUser = new User("John", "Doe", 69, Status.DORMANT);

        List<User> userList = new ArrayList<>();
        userList.add(testUser);
        Map<String, User> userMap = new HashMap<>();
        userMap.put("JD_69", testUser);

        TestModel event = new TestModel(userList, userMap, Status.LIVE);

        /* SEND IT */
        // messageSender.send(event);
    }
}
