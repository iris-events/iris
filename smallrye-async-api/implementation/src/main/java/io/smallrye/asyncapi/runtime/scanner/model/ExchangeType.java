package io.smallrye.asyncapi.runtime.scanner.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ExchangeType {
    DIRECT("direct"),
    FANOUT("fanout"),
    TOPIC("topic");

    private final String type;

    ExchangeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public ExchangeType fromType(String type) {
        List<ExchangeType> collect = Arrays.stream(ExchangeType.values())
                .filter(exchangeType -> exchangeType.getType().equals(type)).collect(Collectors.toList());
        if (collect.isEmpty()) {
            throw new IllegalArgumentException("Unknown type provided");
        }
        return collect.get(0);
    }
}
