package id.global.iris.messaging.runtime.api.message;

import id.global.common.iris.annotations.ExchangeType;
import id.global.common.iris.annotations.Message;

@Message(name = "subscription", exchangeType = ExchangeType.TOPIC)
public record ResourceMessage(String resourceType, String resourceId, Object payload) {
}
