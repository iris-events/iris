package id.global.iris.messaging.runtime.api.message;

import id.global.common.annotations.iris.ExchangeType;
import id.global.common.annotations.iris.Message;

@Message(name = "resource-update", exchangeType = ExchangeType.TOPIC)
public record ResourceUpdate(String resourceType, String resourceId, Object payload) {
}
