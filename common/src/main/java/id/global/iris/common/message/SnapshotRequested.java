package id.global.iris.common.message;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.GlobalIdGenerated;
import id.global.iris.common.annotations.Message;

@GlobalIdGenerated
@Message(name = "snapshot-requested", exchangeType = ExchangeType.TOPIC)
public record SnapshotRequested(String resourceType, String resourceId) {
}
