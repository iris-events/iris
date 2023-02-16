package id.global.iris.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.IrisGenerated;
import id.global.iris.common.annotations.Message;

@IrisGenerated
@Message(name = "snapshot-requested", exchangeType = ExchangeType.TOPIC)
public record SnapshotRequested(@JsonProperty("resource_type") String resourceType,
                                @JsonProperty("resource_id") String resourceId) {
}
