package org.iris_events.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;

@IrisGenerated
@Message(name = "snapshot-requested", exchangeType = ExchangeType.TOPIC)
public record SnapshotRequested(@JsonProperty("resource_type") String resourceType,
                                @JsonProperty("resource_id") String resourceId) {
}
