package org.iris_events.common.message;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Message;

import com.fasterxml.jackson.annotation.JsonProperty;

@Message(name = "subscription", exchangeType = ExchangeType.TOPIC)
public record ResourceMessage(@JsonProperty("resource_type") String resourceType,
        @JsonProperty("resource_id") String resourceId,
        @JsonProperty("payload") Object payload) {
}
