package id.global.event.messaging.runtime.producer;

public enum ExchangeType {
    DIRECT("direct"),
    TOPIC("topic"),
    FANOUT("fanout");

    private final String type;

    ExchangeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
