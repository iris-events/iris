package org.iris_events.runtime;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExchangeNameProvider {

    public String getRpcRequestExchangeName(final String eventName) {
        return String.format("%s.rpc.request", eventName);
    }

    public String getRpcResponseExchangeName(final String eventName) {
        return String.format("%s.rpc.response", eventName);
    }
}
