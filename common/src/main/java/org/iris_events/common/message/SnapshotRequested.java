package org.iris_events.common.message;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;

import com.fasterxml.jackson.annotation.JsonProperty;

@IrisGenerated
@Message(name = "snapshot-requested", exchangeType = ExchangeType.TOPIC)
public record SnapshotRequested(@JsonProperty("resource_type") String resourceType,
        @JsonProperty("resource_id") String resourceId) {
}
