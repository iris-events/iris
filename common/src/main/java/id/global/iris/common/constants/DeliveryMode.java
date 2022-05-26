package id.global.iris.common.constants;

public enum DeliveryMode {
    NON_PERSISTENT(1),
    PERSISTENT(2);

    private final int value;

    DeliveryMode(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
