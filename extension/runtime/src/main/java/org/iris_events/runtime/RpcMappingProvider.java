package org.iris_events.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.DotName;

@ApplicationScoped
public class RpcMappingProvider {
    private Map<String, String> mapping;

    public RpcMappingProvider() {
        this.mapping = new HashMap<>();
    }

    public void addReplyToMappings(final Map<String, String> rpcReplyToMapping) {
        this.mapping.putAll(rpcReplyToMapping);
    }

    public String getReplyTo(DotName type) {
        return mapping.get(type.toString());
    }
}
