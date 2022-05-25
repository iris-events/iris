package id.global.iris.common.message;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Message;

@Message(name = "subscription", exchangeType = ExchangeType.TOPIC)
public record ResourceMessage(String resourceType, String resourceId, Object payload) {
}
