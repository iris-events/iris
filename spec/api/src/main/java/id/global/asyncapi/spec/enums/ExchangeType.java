package id.global.asyncapi.spec.enums;

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

    public static ExchangeType fromType(String type) {
        for (ExchangeType exchangeType : ExchangeType.values()) {
            if (exchangeType.getType().equals(type) || exchangeType.toString().equals(type)) {
                return exchangeType;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown type provided %s", type));
    }
}
