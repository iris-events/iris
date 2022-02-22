package id.global.common.iris;

@SuppressWarnings("unused")
public enum Exchanges {
    DEAD_LETTER(Constants.DEAD_LETTER),
    ERROR("error"),
    FRONTEND("frontend"),
    RETRY("retry"),
    ROADCAST("broadcast"),
    SESSION("session"),
    USER("user");

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
    }
}
