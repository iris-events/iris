package id.global.common.iris.message;

import id.global.common.iris.annotations.ExchangeType;
import id.global.common.iris.annotations.GlobalIdGenerated;
import id.global.common.iris.annotations.Message;

@GlobalIdGenerated
@Message(name = "snapshot-requested", exchangeType = ExchangeType.TOPIC)
public record SnapshotRequested(String resourceType, String resourceId) {
}
