package org.iris_events.common;

@SuppressWarnings("unused")
public enum Exchanges {
    DEAD_LETTER(Constants.DEAD_LETTER),
    ERROR("error"),
    FRONTEND("frontend"),
    RETRY("retry"),
    BROADCAST("broadcast"),
    SESSION("session"),
    USER("user"),
    SUBSCRIPTION("subscription"),
    IDENTITY_AUTHENTICATED(Constants.IDENTITY_AUTHENTICATED),
    SNAPSHOT_REQUESTED("snapshot-requested");

    private final String value;

    Exchanges(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String DEAD_LETTER_PREFIX = "dead.";
        public static final String DEAD_LETTER = DEAD_LETTER_PREFIX + "dead-letter";
        public static final String IDENTITY_AUTHENTICATED = "identity/authenticated";
    }
}
