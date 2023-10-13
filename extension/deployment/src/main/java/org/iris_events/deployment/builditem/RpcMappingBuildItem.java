package org.iris_events.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

public final class RpcMappingBuildItem extends MultiBuildItem {
    private final Map<String, String> rpcReplyToMapping;

    public RpcMappingBuildItem(final Map<String, String> rpcReplyToMapping) {
        this.rpcReplyToMapping = rpcReplyToMapping;
    }

    public Map<String, String> getRpcReplyToMapping() {
        return rpcReplyToMapping;
    }
}
