package id.global.iris.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Message;

@Message(name = "subscription", exchangeType = ExchangeType.TOPIC)
public record ResourceMessage(@JsonProperty("resource_type") String resourceType, @JsonProperty("resource_id") String resourceId,
                              @JsonProperty("payload") Object payload) {
}
